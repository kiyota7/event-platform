package com.example.eventplatform.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findAllByOrderByStartAtAsc(Pageable pageable);

    /** 申込・キャンセルのたびにversionを必ず進め、同時更新を楽観的ロックで検出する */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findForRegistration(@Param("id") Long id);
}
