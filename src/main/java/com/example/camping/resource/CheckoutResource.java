package com.example.camping.resource;

import com.example.camping.dto.OrderPayload;
import com.example.camping.observability.InstanaTracing;
import com.example.camping.service.KafkaCheckoutService;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.instana.sdk.support.ContextSupport;
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

@Path("/checkout")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class CheckoutResource {
    @Inject
    KafkaCheckoutService kafkaCheckoutService;

    @Resource
    ManagedExecutorService executorService;

    @POST
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.CHECKOUT_HTTP_SPAN, captureArguments = true, capturedStackFrames = 5)
    public Map<String, String> receiveCheckout(@Valid @TagParam("order") OrderPayload order) {
        InstanaTracing.httpEntry(InstanaTracing.CHECKOUT_HTTP_SPAN, "POST", "/api/checkout", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.CHECKOUT_HTTP_SPAN, CheckoutResource.class.getName(), "receiveCheckout");
        InstanaTracing.entry(InstanaTracing.CHECKOUT_HTTP_SPAN, "tags.checkout.event_id", order.getEventId());
        InstanaTracing.entry(InstanaTracing.CHECKOUT_HTTP_SPAN, "tags.checkout.order_id", order.getOrderId());
        InstanaTracing.entry(InstanaTracing.CHECKOUT_HTTP_SPAN, "tags.checkout.amount", String.valueOf(order.getAmount()));
        Object snapshotKey = ContextSupport.takeSnapshot();
        executorService.submit(() -> {
            ContextSupport.restoreSnapshot(snapshotKey);
            runCheckoutJob(order);
        });
        return Map.of("status", "success", "message", "Checkout event accepted");
    }

    @Span(type = Span.Type.ENTRY, value = InstanaTracing.CHECKOUT_ASYNC_JOB_SPAN, captureArguments = true, capturedStackFrames = 5)
    private void runCheckoutJob(@TagParam("order") OrderPayload order) {
        InstanaTracing.batchJob(InstanaTracing.CHECKOUT_ASYNC_JOB_SPAN, "checkout-kafka-producer");
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.CHECKOUT_ASYNC_JOB_SPAN, CheckoutResource.class.getName(), "runCheckoutJob");
        try {
            kafkaCheckoutService.send(order);
        } catch (RuntimeException e) {
            InstanaTracing.error(Span.Type.ENTRY, InstanaTracing.CHECKOUT_ASYNC_JOB_SPAN, e);
            throw e;
        }
    }
}
