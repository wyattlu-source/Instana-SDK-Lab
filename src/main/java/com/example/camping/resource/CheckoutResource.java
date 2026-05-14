package com.example.camping.resource;

import com.example.camping.dto.OrderPayload;
import com.example.camping.service.KafkaCheckoutService;
import com.example.camping.util.InstanaTracingUtil;
import com.instana.sdk.annotation.Span;
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

import java.util.Map;
import java.util.logging.Logger;

/**
 * Checkout Resource
 *
 * 追蹤層級：
 * 1. Instana Agent 自動追蹤：HTTP 請求、方法調用、異常
 * 2. SDK 手動追蹤：業務邏輯步驟、訂單資訊、處理狀態
 */
@Path("/checkout")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class CheckoutResource {
    private static final Logger LOGGER = Logger.getLogger(CheckoutResource.class.getName());

    @Inject
    KafkaCheckoutService kafkaCheckoutService;

    @Resource
    ManagedExecutorService executorService;

    /**
     * Agent 自動追蹤：HTTP POST 請求、請求參數和回應、執行時間
     * SDK 手動追蹤：訂單詳細資訊、業務處理步驟、自訂業務標籤
     */
    @POST
    @Span(value = "checkout.receive", type = Span.Type.ENTRY)
    public Map<String, String> receiveCheckout(@Valid OrderPayload order) {
        return InstanaTracingUtil.trace("CheckoutResource.receiveCheckout", () -> {
            
            // === 步驟 1：驗證和記錄訂單資訊 ===
            InstanaTracingUtil.markStep("1.validate_order", "驗證訂單資料");
            
            // 添加業務標籤（Agent 不會自動記錄這些業務資訊）
            InstanaTracingUtil.addBusinessTag("order.id", order.getOrderId());
            InstanaTracingUtil.addBusinessTag("order.event_id", order.getEventId());
            InstanaTracingUtil.addBusinessTag("order.product_id", order.getProductId());
            InstanaTracingUtil.addBusinessTag("order.product_name", order.getProductName());
            InstanaTracingUtil.addBusinessTag("order.amount", order.getAmount());
            InstanaTracingUtil.addBusinessTag("order.user_email", order.getUserEmail());
            InstanaTracingUtil.addBusinessTag("order.funnel_step", order.getFunnelStep());
            
            // 記錄業務事件
            InstanaTracingUtil.logBusinessEvent("ORDER_RECEIVED",
                String.format("Order %s received for product %s",
                    order.getOrderId(), order.getProductId()));
            
            // === 步驟 2：提交非同步處理 ===
            InstanaTracingUtil.markStep("2.submit_async", "提交非同步 Kafka 處理");
            
            InstanaTracingUtil.traceVoid("CheckoutResource.submitAsyncTask", () -> {
                executorService.submit(() -> {
                    // Agent 會追蹤這個非同步執行
                    InstanaTracingUtil.traceVoid("CheckoutResource.asyncExecution", () -> {
                        kafkaCheckoutService.send(order);
                    });
                });
            });
            
            InstanaTracingUtil.logBusinessEvent("ASYNC_TASK_SUBMITTED",
                "Order processing task submitted to executor");
            
            // === 步驟 3：準備回應 ===
            InstanaTracingUtil.markStep("3.prepare_response", "準備成功回應");
            
            Map<String, String> response = Map.of(
                "status", "success",
                "message", "訂單已接收並進入處理管線"
            );
            
            InstanaTracingUtil.addBusinessTag("response.status", "success");
            InstanaTracingUtil.logBusinessEvent("ORDER_RESPONSE_SENT", "Success response sent to client");
            
            return response;
        });
    }
}
