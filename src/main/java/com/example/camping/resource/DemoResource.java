package com.example.camping.resource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Path("/demo")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class DemoResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoResource.class);

    // spot-service 服務未啟動：使用不存在的 port 9999 → 立即 connection refused
    private static final String SPOT_DOWN_URL = "http://10.107.85.67:9999/spot-service/api";

    // MongoDB 帳號密碼錯誤：相同 Atlas 主機，但用錯誤的 user/pass
    private static final String MONGO_WRONG_CREDS =
            "mongodb://wrong_user:WrongP%40ss999@" +
            "ac-llvix9x-shard-00-00.v90cyby.mongodb.net:27017," +
            "ac-llvix9x-shard-00-01.v90cyby.mongodb.net:27017," +
            "ac-llvix9x-shard-00-02.v90cyby.mongodb.net:27017" +
            "/?authSource=admin&replicaSet=atlas-lgdvwi-shard-0" +
            "&tls=true&appName=Cluster0&serverSelectionTimeoutMS=6000";

    // MongoDB 服務未啟動：本機 port 不存在 → 立即 connection refused / timeout
    private static final String MONGO_DOWN_URI =
            "mongodb://wyattlu25_db_user:dummy@10.107.85.67:27099/camping" +
            "?serverSelectionTimeoutMS=4000";

    // ── Scenario 1: spot-service 服務未啟動 ──────────────────────────────────
    @GET
    @Path("/spot-error/service-down")
    public Response spotServiceDown() {

        LOGGER.info("[DEMO] triggering spot-service connection-refused at {}", SPOT_DOWN_URL);

        Client client = ClientBuilder.newBuilder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        try {
            client.target(SPOT_DOWN_URL)
                    .path("/spots")
                    .request(MediaType.APPLICATION_JSON)
                    .get(String.class);
            return Response.ok(Map.of("status", "unexpected_success")).build();
        } catch (Exception e) {
            return handleSpotError(e, SPOT_DOWN_URL);
        } finally {
            client.close();
        }
    }

    // ── Scenario 2: MongoDB 帳號密碼錯誤 ─────────────────────────────────────
    @GET
    @Path("/mongo-error/auth-failure")
    public Response mongoAuthFailure() {

        LOGGER.info("[DEMO] triggering MongoDB auth failure with wrong credentials");
        return runMongoProbe(MONGO_WRONG_CREDS);
    }

    // ── Scenario 3: MongoDB 服務未啟動 ────────────────────────────────────────
    @GET
    @Path("/mongo-error/service-down")
    public Response mongoServiceDown() {

        LOGGER.info("[DEMO] triggering MongoDB service-down at port 27099");
        return runMongoProbe(MONGO_DOWN_URI);
    }

    // ── 內部工具方法 ──────────────────────────────────────────────────────────

    private Response handleSpotError(Exception e, String targetUrl) {
        String errorType  = classifySpotError(e);
        String hint       = spotErrorHint(errorType);
        String causeClass = e.getCause() != null ? e.getCause().getClass().getSimpleName() : "none";

        LOGGER.error("[DEMO] spot-service error [{}] at {}: {}", errorType, targetUrl, e.getMessage());

        // ── SDK：詳細標籤，在 Instana 瀑布圖中清楚可見 ──

        Map<String, Object> body = new HashMap<>();
        body.put("status",       "error");
        body.put("scenario",     "spot-service-unreachable");
        body.put("error_type",   errorType);
        body.put("error_hint",   hint);
        body.put("target_url",   targetUrl);
        body.put("error_detail", e.getMessage());
        return Response.status(503).entity(body).build();
    }

    private Response runMongoProbe(String uri) {
        try (MongoClient mc = MongoClients.create(uri)) {
            mc.getDatabase("camping").runCommand(new Document("ping", 1));
            return Response.ok(Map.of("status", "unexpected_success")).build();
        } catch (Exception e) {
            String errorType  = classifyMongoError(e);
            String hint       = mongoErrorHint(errorType);
            String causeClass = e.getCause() != null ? e.getCause().getClass().getSimpleName() : "none";

            LOGGER.error("[DEMO] MongoDB error [{}]: {}", errorType, e.getMessage());

            // ── SDK：詳細標籤 ──

            Map<String, Object> body = new HashMap<>();
            body.put("status",       "error");
            body.put("error_type",   errorType);
            body.put("error_hint",   hint);
            body.put("error_detail", e.getMessage());
            return Response.status(503).entity(body).build();
        }
    }

    // ── 錯誤分類：spot-service ─────────────────────────────────────────────

    private String classifySpotError(Exception e) {
        Throwable t = e;
        while (t != null) {
            String cn  = t.getClass().getSimpleName().toLowerCase();
            String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
            if (cn.contains("connectexception")    || msg.contains("connection refused")) return "connection_refused";
            if (cn.contains("sockettimeoutexception") || msg.contains("timed out"))       return "timeout";
            if (cn.contains("unknownhostexception"))                                      return "unknown_host";
            if (msg.contains("401") || msg.contains("unauthorized"))                     return "unauthorized";
            t = t.getCause();
        }
        return "unknown";
    }

    private String spotErrorHint(String type) {
        switch (type) {
            case "connection_refused": return "spot-service 服務未啟動（Connection refused），請確認服務是否運行中";
            case "timeout":           return "spot-service 連線逾時，服務可能過載或未回應";
            case "unknown_host":      return "無法解析 spot-service 主機名稱，請檢查 SPOT_SERVICE_URL 設定";
            case "unauthorized":      return "spot-service 認證失敗（HTTP 401），請確認 API Key 或帳號密碼";
            default:                  return "spot-service 呼叫失敗，請查看伺服器日誌取得詳情";
        }
    }

    // ── 錯誤分類：MongoDB ──────────────────────────────────────────────────

    private String classifyMongoError(Exception e) {
        Throwable t = e;
        while (t != null) {
            String cn  = t.getClass().getName().toLowerCase();
            String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
            if (cn.contains("authenticationexception")
                    || msg.contains("authentication failed")
                    || msg.contains("bad auth")
                    || msg.contains("sasl")
                    || msg.contains("credential")) return "auth_failure";
            if (cn.contains("mongotimeoutexception")
                    || cn.contains("sockettimeoutexception")
                    || msg.contains("timed out")
                    || msg.contains("server selection timeout")) return "timeout";
            if (cn.contains("connectexception")
                    || msg.contains("connection refused")) return "connection_refused";
            t = t.getCause();
        }
        return "unknown";
    }

    private String mongoErrorHint(String type) {
        switch (type) {
            case "auth_failure":       return "MongoDB 帳號密碼錯誤，請確認 MONGODB_URI 中的用戶名稱與密碼是否正確";
            case "timeout":            return "MongoDB 連線逾時（serverSelectionTimeout），服務可能未啟動或主機不可達";
            case "connection_refused": return "MongoDB 服務未啟動（Connection refused），請確認 mongod 服務狀態";
            default:                   return "MongoDB 連線失敗，請查看伺服器日誌取得詳情";
        }
    }
}
