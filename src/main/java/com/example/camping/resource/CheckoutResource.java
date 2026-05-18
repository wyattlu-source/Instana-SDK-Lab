package com.example.camping.resource;

import com.example.camping.dto.OrderPayload;
import com.example.camping.model.Coupon;
import com.example.camping.model.CouponStatus;
import com.example.camping.observability.InstanaTracing;
import com.example.camping.repository.CouponRepository;
import com.example.camping.service.AuditService;
import com.example.camping.service.KafkaCheckoutService;
import com.example.camping.service.OrderValidateService;
import com.example.camping.service.PricingService;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.instana.sdk.support.ContextSupport;
import com.instana.sdk.support.SpanSupport;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/checkout")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class CheckoutResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckoutResource.class);

    @Inject KafkaCheckoutService kafkaCheckoutService;
    @Inject OrderValidateService orderValidateService;
    @Inject PricingService pricingService;
    @Inject AuditService auditService;
    @Inject CouponRepository couponRepository;

    @Resource
    ManagedExecutorService executorService;

    @POST
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.CHECKOUT_HTTP_SPAN, capturedStackFrames = 5)
    public Map<String, String> receiveCheckout(@Valid @TagParam("order") OrderPayload order) {
        InstanaTracing.httpEntry(InstanaTracing.CHECKOUT_HTTP_SPAN, "POST", "/api/checkout", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.CHECKOUT_HTTP_SPAN,
                CheckoutResource.class.getName(), "receiveCheckout");

        LOGGER.warn("[CHECKOUT] received - event_id: " + order.getEventId() + " order_id: " + order.getOrderId() + " user: " + order.getUserEmail());

        // ① 驗證訂單 → sdk.camping-order-validate
        orderValidateService.validate(order);

        // ② 計算價格（內部呼叫 spot-service）→ sdk.camping-pricing-calculate
        //    → 這會再產生 spot-service 的 span！
        int total = pricingService.calculateTotal(order.getProductId(), 1);
        int discountAmount = 0;
        int finalTotal = total;

        // ③ 驗證並使用優惠券（如果提供）
        String couponCode = order.getCouponCode();
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            LOGGER.warn("[CHECKOUT] validating coupon - coupon_code: " + couponCode);
            SpanSupport.annotate("tags.checkout.coupon_code", couponCode);

            try {
                // 查詢優惠券
                Optional<Coupon> couponOpt = couponRepository.findByCouponCode(couponCode);
                
                if (!couponOpt.isPresent()) {
                    LOGGER.warn("[CHECKOUT] coupon not found - coupon_code: " + couponCode);
                    throw new jakarta.ws.rs.BadRequestException("優惠券不存在");
                }

                Coupon coupon = couponOpt.get();

                // 驗證優惠券狀態
                if (coupon.getStatus() != CouponStatus.UNUSED) {
                    LOGGER.warn("[CHECKOUT] coupon already used or expired - coupon_code: " + couponCode +
                            " status: " + coupon.getStatus());
                    throw new jakarta.ws.rs.BadRequestException("優惠券已使用或已過期");
                }

                // 驗證優惠券是否過期
                long now = System.currentTimeMillis();
                if (coupon.getExpiresAt() <= now) {
                    LOGGER.warn("[CHECKOUT] coupon expired - coupon_code: " + couponCode +
                            " expires_at: " + coupon.getExpiresAt());
                    throw new jakarta.ws.rs.BadRequestException("優惠券已過期");
                }

                // 使用優惠券（原子更新）
                boolean used = couponRepository.useCoupon(couponCode, order.getOrderId());
                if (!used) {
                    LOGGER.warn("[CHECKOUT] failed to use coupon - coupon_code: " + couponCode);
                    throw new jakarta.ws.rs.BadRequestException("優惠券使用失敗,可能已被使用或已過期");
                }

                // 計算折扣後的價格
                discountAmount = coupon.getDiscountAmount();
                finalTotal = Math.max(0, total - discountAmount);

                LOGGER.warn("[CHECKOUT] coupon applied - coupon_code: " + couponCode +
                        " discount: " + discountAmount + " final_total: " + finalTotal);
                SpanSupport.annotate("tags.checkout.discount_amount", String.valueOf(discountAmount));
                SpanSupport.annotate("tags.checkout.coupon_applied", "true");

            } catch (jakarta.ws.rs.BadRequestException e) {
                // 重新拋出業務邏輯錯誤
                throw e;
            } catch (Exception e) {
                LOGGER.error("[CHECKOUT] coupon validation error - coupon_code: " + couponCode, e);
                throw new jakarta.ws.rs.ServiceUnavailableException("優惠券驗證失敗,請稍後再試");
            }
        }

        SpanSupport.annotate("tags.checkout.total", String.valueOf(total));
        SpanSupport.annotate("tags.checkout.final_total", String.valueOf(finalTotal));

        // ④ 審計記錄（共用方法）→ sdk.camping-audit-record
        auditService.record("checkout", order.getUserEmail());

        SpanSupport.annotate("tags.checkout.event_id", order.getEventId());
        SpanSupport.annotate("tags.checkout.order_id", order.getOrderId());

        // ⑤ 非同步送 Kafka → EXIT span
        Object snapshotKey = ContextSupport.takeSnapshot();
        executorService.submit(() -> {
            ContextSupport.restoreSnapshot(snapshotKey);
            runCheckoutJob(order);
        });

        LOGGER.warn("[CHECKOUT] accepted - event_id: " + order.getEventId() +
                " total: " + total + " discount: " + discountAmount + " final_total: " + finalTotal);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Checkout event accepted");
        response.put("total", String.valueOf(total));
        response.put("discount_amount", String.valueOf(discountAmount));
        response.put("final_total", String.valueOf(finalTotal));
        
        return response;
    }

    @Span(type = Span.Type.ENTRY, value = InstanaTracing.CHECKOUT_ASYNC_JOB_SPAN, capturedStackFrames = 5)
    private void runCheckoutJob(@TagParam("order") OrderPayload order) {
        InstanaTracing.batchJob(InstanaTracing.CHECKOUT_ASYNC_JOB_SPAN, "checkout-kafka-producer");
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.CHECKOUT_ASYNC_JOB_SPAN,
                CheckoutResource.class.getName(), "runCheckoutJob");
        LOGGER.warn("[CHECKOUT] async job started - event_id: " + order.getEventId());
        try {
            kafkaCheckoutService.send(order);
            LOGGER.warn("[CHECKOUT] async job completed - event_id: " + order.getEventId());
        } catch (RuntimeException e) {
            LOGGER.error("[CHECKOUT] async job failed - event_id: " + order.getEventId() + " error: " + e.getMessage(), e);
            InstanaTracing.error(Span.Type.ENTRY, InstanaTracing.CHECKOUT_ASYNC_JOB_SPAN, e);
            throw e;
        }
    }
}
