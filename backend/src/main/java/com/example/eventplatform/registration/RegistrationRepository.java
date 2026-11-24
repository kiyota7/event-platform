package com.example.eventplatform.registration;

import com.example.eventplatform.registration.Registration.Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Optional<Registration> findByEventIdAndUserId(Long eventId, Long userId);
    Optional<Registration> findFirstByEventIdAndStatusOrderByQueueOrderAsc(Long eventId, Status status);
    List<Registration> findByUserIdAndStatusNotOrderByIdDesc(Long userId, Status status);
    long countByEventIdAndStatus(Long eventId, Status status);
    void deleteByEventId(Long eventId);

    @Query("select coalesce(max(r.queueOrder), 0) from Registration r where r.eventId = :eventId")
    long maxQueueOrder(@Param("eventId") Long eventId);

    /** 開始がnow〜limitの範囲で、リマインダー未送信の確定済み申込 */
    @Query("select r from Registration r, Event e where r.eventId = e.id and r.status = 'CONFIRMED' "
            + "and r.reminderSentAt is null and e.startAt > :now and e.startAt <= :limit order by r.id")
    List<Registration> findDueForReminder(@Param("now") java.time.Instant now, @Param("limit") java.time.Instant limit);

    List<Registration> findByEventIdOrderByIdAsc(Long eventId);

    /** [eventId, status, count] */
    @Query("select r.eventId, r.status, count(r) from Registration r where r.eventId in :ids group by r.eventId, r.status")
    List<Object[]> countByStatusForEvents(@Param("ids") java.util.Collection<Long> ids);
}
