package com.example.eventplatform.notification;

/** 参加が確定した(新規申込で確定、またはキャンセル待ちから繰上げ) */
public record RegistrationConfirmedEvent(Long eventId, Long userId, boolean promotedFromWaitlist) {}
