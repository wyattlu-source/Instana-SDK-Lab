package com.example.camping.resource;

import com.example.camping.model.AuthenticatedUser;
import com.example.camping.model.Order;
import com.example.camping.repository.OrderRepository;
import com.example.camping.repository.ProcessingStatusRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class OrderResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderResource.class);

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProcessingStatusRepository processingStatusRepository;

    @Inject
    AuthenticatedUser authenticatedUser;

    @GET
    public Map<String, Object> listOrders() {
        String userId = authenticatedUser.getUserId();
        if (userId == null || userId.isEmpty()) {
            LOGGER.error("[ORDER] userId not found in request context");
            throw new jakarta.ws.rs.NotAuthorizedException("未授權的請求");
        }

        LOGGER.warn("[ORDER] list - userId: " + userId);
        List<Order> orders = orderRepository.findByUserId(userId);

        List<Map<String, Object>> orderList = new ArrayList<>();
        for (Order o : orders) {
            Map<String, Object> item = new HashMap<>();
            item.put("order_id", o.getOrderId());
            item.put("spot_id", o.getSpotId());
            item.put("spot_name", o.getSpotName());
            item.put("check_in_date", o.getCheckInDate());
            item.put("check_out_date", o.getCheckOutDate());
            item.put("nights", o.getNights());
            item.put("unit_price", o.getUnitPrice());
            item.put("total", o.getTotal());
            item.put("discount_amount", o.getDiscountAmount());
            item.put("final_total", o.getFinalTotal());
            item.put("coupon_code", o.getCouponCode());
            item.put("status", o.getStatus());
            item.put("created_at", o.getCreatedAt());
            orderList.add(item);
        }

        LOGGER.warn("[ORDER] found " + orderList.size() + " orders for userId: " + userId);
        return Map.of(
                "success", true,
                "orders", orderList,
                "count", orderList.size()
        );
    }

    @DELETE
    @Path("/{orderId}")
    public Response cancelOrder(@PathParam("orderId") String orderId) {
        String userId = authenticatedUser.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new jakarta.ws.rs.NotAuthorizedException("未授權的請求");
        }
        LOGGER.warn("[ORDER] cancel - orderId: " + orderId + " userId: " + userId);
        boolean cancelled = orderRepository.cancelOrder(orderId, userId);
        if (!cancelled) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("success", false, "error", "訂單不存在或無權取消")).build();
        }
        return Response.ok(Map.of("success", true, "message", "訂單已取消")).build();
    }

    @GET
    @Path("/{orderId}/status")
    public Map<String, Object> getProcessingStatus(@PathParam("orderId") String orderId) {
        String userId = authenticatedUser.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new jakarta.ws.rs.NotAuthorizedException("未授權的請求");
        }
        Map<String, Object> status = processingStatusRepository.findByOrderId(orderId);
        if (status == null) {
            return Map.of("success", false, "message", "處理結果尚未就緒，請稍後再試");
        }
        return Map.of("success", true, "status", status);
    }
}
