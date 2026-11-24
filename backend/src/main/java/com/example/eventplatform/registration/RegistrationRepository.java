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
}
