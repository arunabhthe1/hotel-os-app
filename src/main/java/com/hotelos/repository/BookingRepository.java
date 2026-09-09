package com.hotelos.repository;

import com.hotelos.domain.Booking;
import com.hotelos.domain.User;
import com.hotelos.domain.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByPublicId(String publicId);

    @Query("""
            select b from Booking b
            join fetch b.room
            join fetch b.order
            where b.user = :user
            order by b.checkIn desc
            """)
    List<Booking> findByUser(@Param("user") User user);

    @Query("""
            select b from Booking b
            join fetch b.room
            join fetch b.order
            where b.status <> :cancelled
            order by b.checkIn
            """)
    List<Booking> findActive(@Param("cancelled") BookingStatus cancelled);
}
