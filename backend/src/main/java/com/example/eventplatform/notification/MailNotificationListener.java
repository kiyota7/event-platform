package com.example.eventplatform.notification;

import com.example.eventplatform.event.Event;
import com.example.eventplatform.event.EventRepository;
import com.example.eventplatform.user.User;
import com.example.eventplatform.user.UserRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** 参加確定メール。コミット後に非同期で送る(送信失敗が申込を巻き戻さない) */
@Component
public class MailNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(MailNotificationListener.class);
    private final MailSender mail;
    private final EventRepository events;
    private final UserRepository users;

    public MailNotificationListener(MailSender mail, EventRepository events, UserRepository users) {
        this.mail = mail; this.events = events; this.users = users;
    }

    @Async
    @TransactionalEventListener
    public void onConfirmed(RegistrationConfirmedEvent ev) {
        try {
            Event e = events.findById(ev.eventId()).orElse(null);
            User u = users.findById(ev.userId()).orElse(null);
            if (e == null || u == null) return;
            String subject = (ev.promotedFromWaitlist() ? "【繰上げ参加確定】" : "【参加確定】") + e.getTitle();
            String lead = ev.promotedFromWaitlist() ? "キャンセル待ちから繰り上がり、参加が確定しました。" : "参加申込が確定しました。";
            mail.send(u.getEmail(), subject, u.getDisplayName() + " さん\n\n" + lead + "\n\n" + describe(e, u));
        } catch (RuntimeException ex) {
            log.warn("failed to send confirmation mail: event={} user={}", ev.eventId(), ev.userId(), ex);
        }
    }

    /** 開催日時はユーザーのタイムゾーンで表示する */
    static String describe(Event e, User u) {
        ZoneId zone = ZoneId.of(u.getTimeZone());
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm (z)", Locale.JAPAN);
        return "イベント: " + e.getTitle() + "\n開始: " + e.getStartAt().atZone(zone).format(f)
                + "\n終了: " + e.getEndAt().atZone(zone).format(f)
                + (e.getLocation() == null ? "" : "\n場所: " + e.getLocation());
    }
}
