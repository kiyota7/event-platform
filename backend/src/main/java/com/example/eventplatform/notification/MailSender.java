package com.example.eventplatform.notification;

public interface MailSender {
    void send(String to, String subject, String body);
}
