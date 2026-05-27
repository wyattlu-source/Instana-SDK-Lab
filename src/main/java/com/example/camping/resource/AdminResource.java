package com.example.camping.resource;

import com.example.camping.model.Coupon;
import com.example.camping.model.Order;
import com.example.camping.repository.CouponRepository;
import com.example.camping.repository.OrderRepository;
import com.example.camping.repository.ProcessingStatusRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class AdminResource {

    private static final String ADMIN_KEY = "camping-admin-2026";

    @Inject OrderRepository orderRepository;
    @Inject CouponRepository couponRepository;
    @Inject ProcessingStatusRepository processingStatusRepository;

    private void requireAdmin(HttpHeaders headers) {
        String key = headers.getHeaderString("X-Admin-Key");
        if (!ADMIN_KEY.equals(key)) throw new NotAuthorizedException("Invalid admin key");
    }

    @GET
    @Path("/stats")
    public Response stats(@Context HttpHeaders headers) {
        requireAdmin(headers);
        List<Order> orders = orderRepository.findAll();
        List<Coupon> coupons = couponRepository.findAll();
        List<Map<String, Object>> processing = processingStatusRepository.findAll();

        long totalRevenue = orders.stream().mapToLong(o -> o.getFinalTotal()).sum();
        long usedCoupons  = coupons.stream().filter(c -> c.getStatus().getValue().equals("USED")).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_orders",      orders.size());
        stats.put("total_coupons",     coupons.size());
        stats.put("used_coupons",      usedCoupons);
        stats.put("processed_orders",  processing.size());
        stats.put("total_revenue",     totalRevenue);
        return Response.ok(Map.of("success", true, "stats", stats)).build();
    }

    @GET
    @Path("/orders")
    public Response allOrders(@Context HttpHeaders headers) {
        requireAdmin(headers);
        List<Order> orders = orderRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Order o : orders) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("order_id",       o.getOrderId());
            m.put("user_id",        o.getUserId());
            m.put("user_email",     o.getUserEmail());
            m.put("spot_id",        o.getSpotId());
            m.put("spot_name",      o.getSpotName());
            m.put("check_in_date",  o.getCheckInDate());
            m.put("check_out_date", o.getCheckOutDate());
            m.put("nights",         o.getNights());
            m.put("unit_price",     o.getUnitPrice());
            m.put("total",          o.getTotal());
            m.put("discount_amount",o.getDiscountAmount());
            m.put("final_total",    o.getFinalTotal());
            m.put("coupon_code",    o.getCouponCode());
            m.put("status",         o.getStatus());
            m.put("created_at",     o.getCreatedAt());
            list.add(m);
        }
        return Response.ok(Map.of("success", true, "orders", list, "count", list.size())).build();
    }

    @GET
    @Path("/coupons")
    public Response allCoupons(@Context HttpHeaders headers) {
        requireAdmin(headers);
        List<Coupon> coupons = couponRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Coupon c : coupons) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("coupon_id",      c.getCouponId());
            m.put("coupon_code",    c.getCouponCode());
            m.put("user_id",        c.getUserId());
            m.put("spot_id",        c.getSpotId());
            m.put("spot_name",      c.getSpotName());
            m.put("discount_amount",c.getDiscountAmount());
            m.put("status",         c.getStatus().getValue());
            m.put("created_at",     c.getCreatedAt());
            m.put("expires_at",     c.getExpiresAt());
            if (c.getOrderId() != null) m.put("order_id", c.getOrderId());
            list.add(m);
        }
        return Response.ok(Map.of("success", true, "coupons", list, "count", list.size())).build();
    }

    @GET
    @Path("/processing")
    public Response allProcessing(@Context HttpHeaders headers) {
        requireAdmin(headers);
        List<Map<String, Object>> list = processingStatusRepository.findAll();
        return Response.ok(Map.of("success", true, "processing", list, "count", list.size())).build();
    }

    private static final String INSTANA_BASE  = "https://it-palsys.instana-k3s.palsys.com.tw";
    private static final String INSTANA_TOKEN = "9oYyUYH2Qum_QvUHHUr8bA";
    private static final TrustManager[] TRUST_ALL = { new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        public void checkClientTrusted(X509Certificate[] c, String a) {}
        public void checkServerTrusted(X509Certificate[] c, String a) {}
    }};

    @GET
    @Path("/instana-status")
    public Response instanaStatus(@Context HttpHeaders headers) {
        requireAdmin(headers);
        try {
            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(null, TRUST_ALL, new SecureRandom());
            HttpClient http = HttpClient.newBuilder()
                    .sslContext(sslCtx)
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();

            long now = System.currentTimeMillis();
            long oneHourAgo = now - 3_600_000L;

            // Events
            HttpRequest eventsReq = HttpRequest.newBuilder()
                    .uri(URI.create(INSTANA_BASE + "/api/events?from=" + oneHourAgo + "&to=" + now + "&maxResults=25"))
                    .header("Authorization", "apiToken " + INSTANA_TOKEN)
                    .GET().build();
            String eventsBody = http.send(eventsReq, HttpResponse.BodyHandlers.ofString()).body();

            // Service metrics
            String metricsPayload = "{\"metrics\":[{\"metric\":\"calls\",\"aggregation\":\"SUM\"},"
                    + "{\"metric\":\"erroneousCalls\",\"aggregation\":\"SUM\"},"
                    + "{\"metric\":\"latency\",\"aggregation\":\"MEAN\"}],"
                    + "\"timeFrame\":{\"windowSize\":3600000,\"to\":" + now + "},"
                    + "\"group\":{\"groupbyTag\":\"service.name\",\"groupbyTagEntity\":\"DESTINATION\"}}";
            HttpRequest metricsReq = HttpRequest.newBuilder()
                    .uri(URI.create(INSTANA_BASE + "/api/application-monitoring/metrics/services"))
                    .header("Authorization", "apiToken " + INSTANA_TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(metricsPayload)).build();
            String metricsBody = http.send(metricsReq, HttpResponse.BodyHandlers.ofString()).body();

            String combined = "{\"events_raw\":" + eventsBody
                    + ",\"metrics_raw\":" + metricsBody
                    + ",\"fetched_at\":" + now + "}";
            return Response.ok(combined).type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
