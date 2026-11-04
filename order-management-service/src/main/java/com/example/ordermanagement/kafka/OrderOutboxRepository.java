package com.example.ordermanagement.kafka;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface OrderOutboxRepository extends JpaRepository<OrderOutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from OrderOutboxEvent event where event.publishedAt is null order by event.id")
    List<OrderOutboxEvent> findPendingForUpdate(Pageable pageable);
}
