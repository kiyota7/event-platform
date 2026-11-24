package com.example.eventplatform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** SendGridキー未設定時の開発用実装。ログに出すだけ */
@Component
@ConditionalOnExpression("'${app.mail.sendgrid-api-key:}'.isEmpty()")
public class LoggingMailSender implements MailSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[MAIL] to={} subject={}\n{}", to, subject, body);
    }
}
