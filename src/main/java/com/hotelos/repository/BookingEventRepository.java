package com.hotelos.repository;

import com.hotelos.domain.BookingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingEventRepository extends JpaRepository<BookingEvent, Long> {
}
