package com.example.eventplatform.notification;

import com.example.eventplatform.event.Event;
import com.example.eventplatform.event.EventRepository;
import com.example.eventplatform.registration.Registration;
import com.example.eventplatform.registration.RegistrationRepository;
import com.example.eventplatform.user.User;
import com.example.eventplatform.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 開催前リマインダー。開始まで hours-before 時間以内の確定参加者に1回だけ送る */
@Service
public class ReminderService {
    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private final RegistrationRepository registrations;
    private final EventRepository events;
    private final UserRepository users;
    private final MailSender mail;
    private final long hoursBefore;

    public ReminderService(RegistrationRepository registrations, EventRepository events, UserRepository users,
                           MailSender mail, @Value("${app.reminder.hours-before}") long hoursBefore) {
        this.registrations = registrations; this.events = events; this.users = users;
        this.mail = mail; this.hoursBefore = hoursBefore;
    }

    @Scheduled(initialDelay = 60_000, fixedDelayString = "${app.reminder.interval-ms}")
    public void scheduled() { sendDueReminders(Instant.now()); }

    /** 送信件数を返す。送信に失敗した申込は未送信のまま残り、次回再試行される */
    @Transactional
    public int sendDueReminders(Instant now) {
        List<Registration> due = registrations.findDueForReminder(now, now.plus(Duration.ofHours(hoursBefore)));
        int sent = 0;
        for (Registration r : due) {
            try {
                Event e = events.findById(r.getEventId()).orElse(null);
                User u = users.findById(r.getUserId()).orElse(null);
                if (e == null || u == null) continue;
                mail.send(u.getEmail(), "【リマインダー】まもなく開催: " + e.getTitle(),
                        u.getDisplayName() + " さん\n\n参加予定のイベントがまもなく始まります。\n\n" + MailNotificationListener.describe(e, u));
                r.setReminderSentAt(now);
                sent++;
            } catch (RuntimeException ex) {
                log.warn("failed to send reminder: registration={}", r.getId(), ex);
            }
        }
        return sent;
    }
}
