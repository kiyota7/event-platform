package com.example.eventplatform.notification;

import com.example.eventplatform.event.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** 参加確定をアプリ内通知(+SSE)として記録する */
@Component
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private final NotificationService notifications;
    private final EventRepository events;

    public NotificationListener(NotificationService notifications, EventRepository events) {
        this.notifications = notifications; this.events = events;
    }

    @Async
    @TransactionalEventListener
    public void onConfirmed(RegistrationConfirmedEvent ev) {
        try {
            events.findById(ev.eventId()).ifPresent(e -> {
                boolean promoted = ev.promotedFromWaitlist();
                notifications.create(ev.userId(),
                        promoted ? Notification.Type.PROMOTED_FROM_WAITLIST : Notification.Type.REGISTRATION_CONFIRMED,
                        (promoted ? "キャンセル待ちから繰り上がり参加が確定しました: " : "参加が確定しました: ") + e.getTitle(), e.getId());
            });
        } catch (RuntimeException ex) {
            log.warn("failed to create notification: event={} user={}", ev.eventId(), ev.userId(), ex);
        }
    }
}
