package com.hotelos.repository;

import com.hotelos.domain.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByPublicId(String publicId);

    @Query("select distinct r from Room r left join fetch r.amenities where r.publicId = :publicId")
    Optional<Room> findByPublicIdWithAmenities(@Param("publicId") String publicId);

    @Query("select distinct r from Room r left join fetch r.amenities where r.active = true order by r.roomNumber")
    List<Room> findAllActiveWithAmenities();

    @Query("select distinct r from Room r left join fetch r.amenities order by r.roomNumber")
    List<Room> findAllWithAmenities();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
