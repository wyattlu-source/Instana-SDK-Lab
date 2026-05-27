package com.example.camping.resource;

import com.example.camping.dto.OrderPayload;
import com.example.camping.model.Coupon;
import com.example.camping.model.CouponStatus;
import com.example.camping.model.Order;
import com.example.camping.model.AuthenticatedUser;
import com.example.camping.observability.InstanaTracing;
import com.example.camping.repository.CouponRepository;
import com.example.camping.repository.OrderRepository;
import com.example.camping.service.AuditService;
import com.example.camping.service.KafkaCheckoutService;
import com.example.camping.service.OrderValidateService;
import com.example.camping.service.PricingService;
import com.example.camping.service.ReportingService;
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    @Inject OrderRepository orderRepository;
    @Inject AuthenticatedUser authenticatedUser;
    @Inject ReportingService reportingService;

    @Resource
    ManagedExecutorService executorService;

    @POST
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.CHECKOUT_HTTP_SPAN, capturedStackFrames = 5)
    public Map<String, Object> receiveCheckout(@Valid @TagParam("order") OrderPayload order) {
        InstanaTracing.httpEntry(InstanaTracing.CHECKOUT_HTTP_SPAN, "POST", "/api/checkout", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.CHECKOUT_HTTP_SPAN,
                CheckoutResource.class.getName(), "receiveCheckout");

        InstanaTracing.logWarn(LOGGER, "[CHECKOUT] received - event_id: " + order.getEventId() +
                " order_id: " + order.getOrderId() + " user: " + order.getUserEmail());

        // 計算住宿天數
        int nights = calcNights(order.getCheckInDate(), order.getCheckOutDate());
        InstanaTracing.logWarn(LOGGER, "[CHECKOUT] nights: " + nights +
                " check_in: " + order.getCheckInDate() + " check_out: " + order.getCheckOutDate());

        // ① 驗證訂單
        orderValidateService.validate(order);

        // ② 計算價格（呼叫 spot-service）
        int unitPrice = pricingService.getUnitPrice(order.getProductId());
        int total = unitPrice * nights;
        int discountAmount = 0;
        int finalTotal = total;

        // ③ 驗證並使用優惠券
        String couponCode = order.getCouponCode();
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            LOGGER.warn("[CHECKOUT] validating coupon - coupon_code: " + couponCode);
            SpanSupport.annotate("tags.checkout.coupon_code", couponCode);
            try {
                Optional<Coupon> couponOpt = couponRepository.findByCouponCode(couponCode);
                if (!couponOpt.isPresent()) {
                    LOGGER.warn("[CHECKOUT] coupon not found - coupon_code: " + couponCode);
                    throw new jakarta.ws.rs.BadRequestException("優惠券不存在");
                }
                Coupon coupon = couponOpt.get();
                if (coupon.getStatus() != CouponStatus.UNUSED) {
                    LOGGER.warn("[CHECKOUT] coupon already used or expired - coupon_code: " + couponCode);
                    throw new jakarta.ws.rs.BadRequestException("優惠券已使用或已過期");
                }
                long now = System.currentTimeMillis();
                if (coupon.getExpiresAt() <= now) {
                    LOGGER.warn("[CHECKOUT] coupon expired - coupon_code: " + couponCode);
                    throw new jakarta.ws.rs.BadRequestException("優惠券已過期");
                }
                boolean used = couponRepository.useCoupon(couponCode, order.getOrderId());
                if (!used) {
                    LOGGER.warn("[CHECKOUT] failed to use coupon - coupon_code: " + couponCode);
                    throw new jakarta.ws.rs.BadRequestException("優惠券使用失敗，可能已被使用或已過期");
                }
                discountAmount = coupon.getDiscountAmount();
                finalTotal = Math.max(0, total - discountAmount);
                InstanaTracing.logWarn(LOGGER, "[CHECKOUT] coupon applied - coupon_code: " + couponCode +
                        " discount: " + discountAmount + " final_total: " + finalTotal);
                SpanSupport.annotate("tags.checkout.discount_amount", String.valueOf(discountAmount));
                SpanSupport.annotate("tags.checkout.coupon_applied", "true");
            } catch (jakarta.ws.rs.BadRequestException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("[CHECKOUT] coupon validation error - coupon_code: " + couponCode, e);
                throw new jakarta.ws.rs.ServiceUnavailableException("優惠券驗證失敗，請稍後再試");
            }
        }

        SpanSupport.annotate("tags.checkout.total", String.valueOf(total));
        SpanSupport.annotate("tags.checkout.final_total", String.valueOf(finalTotal));

        // ④ 儲存訂單到 MongoDB
        String userId = authenticatedUser.getUserId();
        String spotName = order.getProductName() != null ? order.getProductName() : order.getProductId();
        Order savedOrder = new Order(
                order.getOrderId(), userId, order.getUserEmail(),
                order.getProductId(), spotName,
                order.getCheckInDate(), order.getCheckOutDate(),
                nights, unitPrice, total, discountAmount, finalTotal,
                couponCode, "confirmed", System.currentTimeMillis()
        );
        try {
            orderRepository.save(savedOrder);
        } catch (Exception e) {
            LOGGER.error("[CHECKOUT] failed to save order - orderId: " + order.getOrderId(), e);
        }

        // ⑤ 審計記錄
        auditService.record("checkout", order.getUserEmail());
        SpanSupport.annotate("tags.checkout.event_id", order.getEventId());
        SpanSupport.annotate("tags.checkout.order_id", order.getOrderId());

        // ⑥ 報表與稽核處理（效能瓶頸）
        try {
            reportingService.generateOrderSummary(order.getOrderId());
            reportingService.runAuditMatrix(authenticatedUser.getUserId());
            reportingService.redundantOrderScan(order.getOrderId());
        } catch (Exception e) {
            LOGGER.warn("[CHECKOUT] reporting step failed: {}", e.getMessage());
        }

        // ⑧ 非同步送 Kafka
        Object snapshotKey = ContextSupport.takeSnapshot();
        executorService.submit(() -> {
            ContextSupport.restoreSnapshot(snapshotKey);
            runCheckoutJob(order);
        });

        InstanaTracing.logWarn(LOGGER, "[CHECKOUT] accepted - event_id: " + order.getEventId() +
                " nights: " + nights + " total: " + total +
                " discount: " + discountAmount + " final_total: " + finalTotal);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Checkout event accepted");
        response.put("order_id", order.getOrderId());
        response.put("check_in_date", order.getCheckInDate());
        response.put("check_out_date", order.getCheckOutDate());
        response.put("nights", nights);
        response.put("unit_price", unitPrice);
        response.put("total", total);
        response.put("discount_amount", discountAmount);
        response.put("final_total", finalTotal);
        return response;
    }

    private int calcNights(String checkIn, String checkOut) {
        if (checkIn == null || checkOut == null || checkIn.isBlank() || checkOut.isBlank()) return 1;
        try {
            long days = ChronoUnit.DAYS.between(LocalDate.parse(checkIn), LocalDate.parse(checkOut));
            return (int) Math.max(1, days);
        } catch (Exception e) {
            LOGGER.warn("[CHECKOUT] invalid date format, defaulting to 1 night: " + e.getMessage());
            return 1;
        }
    }

    private void runCheckoutJob(OrderPayload order) {
        LOGGER.info("[CHECKOUT] async job started - event_id: " + order.getEventId());
        try {
            kafkaCheckoutService.send(order);
            LOGGER.info("[CHECKOUT] async job completed - event_id: " + order.getEventId());
        } catch (RuntimeException e) {
            LOGGER.error("[CHECKOUT] async job failed - event_id: " + order.getEventId(), e);
            throw e;
        }
    }
}
