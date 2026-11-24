package com.example.eventplatform.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** SendGrid v3 Mail Send API */
@Component
@ConditionalOnExpression("!'${app.mail.sendgrid-api-key:}'.isEmpty()")
public class SendGridMailSender implements MailSender {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper om;
    private final String apiKey;
    private final String from;

    public SendGridMailSender(ObjectMapper om, @Value("${app.mail.sendgrid-api-key}") String apiKey,
                              @Value("${app.mail.from}") String from) {
        this.om = om; this.apiKey = apiKey; this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            String json = om.writeValueAsString(Map.of(
                    "personalizations", List.of(Map.of("to", List.of(Map.of("email", to)))),
                    "from", Map.of("email", from),
                    "subject", subject,
                    "content", List.of(Map.of("type", "text/plain", "value", body))));
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.sendgrid.com/v3/mail/send"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) throw new IllegalStateException("SendGrid error " + res.statusCode() + ": " + res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("SendGrid request failed", e);
        }
    }
}
