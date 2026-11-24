package com.example.eventplatform.notification;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** ユーザーごとのSSE接続を管理して通知をpushする(単一インスタンス前提) */
@Component
public class SseHub {
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        register(userId, emitter);
        try { emitter.send(SseEmitter.event().name("connected").data("ok")); }
        catch (IOException | IllegalStateException e) { remove(userId, emitter); }
        return emitter;
    }

    public void register(Long userId, SseEmitter emitter) {
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
    }

    public void push(Long userId, NotificationDto dto) {
        for (SseEmitter em : emitters.getOrDefault(userId, List.of())) {
            try { em.send(SseEmitter.event().name("notification").id(String.valueOf(dto.id())).data(dto)); }
            catch (IOException | IllegalStateException e) { remove(userId, em); }
        }
    }

    /** プロキシによる切断を防ぐハートビート */
    @Scheduled(fixedDelay = 25_000)
    void heartbeat() {
        emitters.forEach((userId, list) -> list.forEach(em -> {
            try { em.send(SseEmitter.event().comment("ping")); }
            catch (IOException | IllegalStateException e) { remove(userId, em); }
        }));
    }

    private void remove(Long userId, SseEmitter em) {
        emitters.computeIfPresent(userId, (k, list) -> { list.remove(em); return list.isEmpty() ? null : list; });
    }
}
