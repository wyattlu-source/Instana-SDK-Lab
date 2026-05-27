package com.example.consumer.notification;

import com.example.consumer.notification.config.AppConfig;
import com.example.consumer.notification.model.RawEvent;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.support.SpanSupport;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.of("Asia/Taipei"));

    private final Session session;
    private final boolean enabled;

    public EmailService() {
        String user = AppConfig.getSmtpUser();
        String pass = AppConfig.getSmtpPass();
        this.enabled = !user.isEmpty() && !pass.isEmpty();

        if (!enabled) {
            LOGGER.warn("[EMAIL] SMTP_USER / SMTP_PASS 未設定，email 功能停用（乾跑模式）");
            this.session = null;
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(AppConfig.getSmtpPort()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        this.session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
        LOGGER.warn("[EMAIL] SMTP 已設定 {}:{} 帳號:{}", AppConfig.getSmtpHost(), AppConfig.getSmtpPort(), user);
    }

    @Span(type = Span.Type.EXIT, value = "notification-send-email")
    public void sendOrderConfirmation(RawEvent event) {
        SpanSupport.annotate("notification.type", "order_confirmation");
        SpanSupport.annotate("notification.recipient", event.getUserEmail());
        SpanSupport.annotate("notification.order_id", event.getOrderId());
        SpanSupport.annotate("notification.product", event.getProductName());

        if (!enabled) {
            // 乾跑：只輸出 LOG，不真的寄
            LOGGER.warn("[EMAIL][DRY-RUN] 訂單確認信 → {} | 訂單:{} | 景點:{} | 金額:${}",
                    event.getUserEmail(), event.getOrderId(), event.getProductName(), event.getAmount());
            SpanSupport.annotate("notification.mode", "dry-run");
            return;
        }

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(AppConfig.getEmailFrom(), "🏕 Camping 露營預訂"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(event.getUserEmail()));
            msg.setSubject("🏕 訂單確認：" + (event.getProductName() != null ? event.getProductName() : "露營景點"));

            String html = buildHtml(event);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, "text/html; charset=UTF-8");

            MimeMultipart mp = new MimeMultipart("alternative");
            mp.addBodyPart(htmlPart);
            msg.setContent(mp);

            Transport.send(msg);
            LOGGER.warn("[EMAIL] ✅ 寄出訂單確認信 → {} | 訂單:{}", event.getUserEmail(), event.getOrderId());
            SpanSupport.annotate("notification.status", "sent");
        } catch (Exception e) {
            LOGGER.error("[EMAIL] ❌ 寄信失敗 → {}: {}", event.getUserEmail(), e.getMessage(), e);
            SpanSupport.annotate("notification.status", "failed");
            SpanSupport.annotate("notification.error", e.getMessage());
            throw new RuntimeException("Email send failed", e);
        }
    }

    private String buildHtml(RawEvent event) {
        String name = event.getUserName() != null ? event.getUserName() : event.getUserEmail();
        String spot = event.getProductName() != null ? event.getProductName() : "露營景點";
        String orderId = event.getOrderId() != null ? event.getOrderId() : "—";
        String amount = event.getAmount() != null ? "$" + event.getAmount() : "—";
        String ts = event.getTs() != null ? FMT.format(Instant.ofEpochMilli(event.getTs())) : "—";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/>"
            + "<style>body{font-family:'Noto Sans TC',sans-serif;background:#f5f0e8;margin:0;padding:20px;}"
            + ".card{background:#fff;border-radius:16px;max-width:560px;margin:0 auto;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.1);}"
            + ".header{background:linear-gradient(135deg,#2e3320,#5c6b48);color:#fff;padding:32px 28px;text-align:center;}"
            + ".header h1{margin:0;font-size:1.5rem;} .header p{margin:8px 0 0;opacity:.8;font-size:.9rem;}"
            + ".body{padding:28px;}"
            + ".greeting{font-size:1rem;color:#333;margin-bottom:20px;}"
            + ".info-box{background:#f5f0e8;border-radius:10px;padding:18px;margin:18px 0;border-left:4px solid #5c6b48;}"
            + ".row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px dashed #e0dbd0;font-size:.88rem;}"
            + ".row:last-child{border:none;font-weight:700;color:#5c6b48;font-size:1rem;}"
            + ".row .k{color:#888;} .row .v{color:#333;font-weight:600;}"
            + ".footer{text-align:center;color:#aaa;font-size:.75rem;padding:16px 28px;border-top:1px solid #f0ece4;}"
            + "</style></head><body>"
            + "<div class='card'>"
            + "<div class='header'><div style='font-size:2.5rem'>🏕️</div><h1>訂單確認通知</h1><p>您的露營預訂已成功確認</p></div>"
            + "<div class='body'>"
            + "<p class='greeting'>親愛的 <strong>" + name + "</strong>，您好！<br>感謝您選擇 Camping 露營預訂平台，您的訂單已確認。</p>"
            + "<div class='info-box'>"
            + "<div class='row'><span class='k'>📍 景點</span><span class='v'>" + spot + "</span></div>"
            + "<div class='row'><span class='k'>📋 訂單編號</span><span class='v'>" + orderId + "</span></div>"
            + "<div class='row'><span class='k'>🕐 預訂時間</span><span class='v'>" + ts + "</span></div>"
            + "<div class='row'><span class='k'>💳 應付金額</span><span class='v'>" + amount + "</span></div>"
            + "</div>"
            + "<p style='color:#888;font-size:.82rem;line-height:1.6;margin-top:16px'>"
            + "如有任何問題，請聯絡客服。祝您露營愉快！🌲</p>"
            + "</div>"
            + "<div class='footer'>此信件由系統自動發送，請勿直接回覆。<br>Camping 露營旅遊預訂平台</div>"
            + "</div></body></html>";
    }
}
