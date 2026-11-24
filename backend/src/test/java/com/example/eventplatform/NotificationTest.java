package com.example.eventplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.eventplatform.event.Event;
import com.example.eventplatform.event.EventRepository;
import com.example.eventplatform.notification.MailSender;
import com.example.eventplatform.notification.ReminderService;
import com.example.eventplatform.registration.RegistrationService;
import com.example.eventplatform.user.User;
import com.example.eventplatform.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class NotificationTest {
    @MockBean MailSender mail;
    @Autowired EventRepository events;
    @Autowired UserRepository users;
    @Autowired RegistrationService service;
    @Autowired ReminderService reminders;

    private Event event(int cap, int hoursFromNow) {
        Event e = new Event(1L);
        e.setTitle("通知テスト"); e.setTimeZone("Asia/Tokyo"); e.setCapacity(cap);
        e.setStartAt(Instant.now().plus(hoursFromNow, ChronoUnit.HOURS));
        e.setEndAt(Instant.now().plus(hoursFromNow + 2, ChronoUnit.HOURS));
        return events.save(e);
    }

    @Test
    void confirmationMailOnRegisterAndPromotion() {
        Event e = event(1, 100);
        User a = users.save(new User("na@example.com", "x", "A", "America/New_York"));
        User b = users.save(new User("nb@example.com", "x", "B", null));
        service.register(e.getId(), a.getId());
        verify(mail, timeout(3000)).send(eq("na@example.com"), contains("参加確定"), any());
        service.register(e.getId(), b.getId());          // キャンセル待ち: メールなし
        verify(mail, after(300).never()).send(eq("nb@example.com"), any(), any());
        service.cancel(e.getId(), a.getId());            // bが繰上げ
        verify(mail, timeout(3000)).send(eq("nb@example.com"), contains("繰上げ"), any());
    }

    @Test
    void reminderSentOnceOnlyWithinWindow() {
        Event soon = event(5, 5), far = event(5, 100);
        User u = users.save(new User("nr@example.com", "x", "R", null));
        service.register(soon.getId(), u.getId());
        service.register(far.getId(), u.getId());
        reset(mail);
        assertEquals(1, reminders.sendDueReminders(Instant.now()));
        verify(mail).send(eq("nr@example.com"), contains("リマインダー"), any());
        assertEquals(0, reminders.sendDueReminders(Instant.now()));
    }
}
