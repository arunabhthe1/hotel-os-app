package com.hotelos.repository;

import com.hotelos.domain.RoomNightOccupancy;
import com.hotelos.domain.enums.OccupancySourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RoomNightOccupancyRepository extends JpaRepository<RoomNightOccupancy, Long> {

    boolean existsByRoomIdAndStayDateIn(Long roomId, Collection<LocalDate> stayDates);

    @Query("""
            select o.stayDate from RoomNightOccupancy o
            where o.room.id = :roomId
              and o.stayDate in :stayDates
            """)
    List<LocalDate> findOccupiedDates(@Param("roomId") Long roomId, @Param("stayDates") Collection<LocalDate> stayDates);

    void deleteBySourceTypeAndSourceId(OccupancySourceType sourceType, Long sourceId);
}
