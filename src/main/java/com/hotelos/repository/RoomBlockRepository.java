package com.hotelos.repository;

import com.hotelos.domain.RoomBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomBlockRepository extends JpaRepository<RoomBlock, Long> {

    Optional<RoomBlock> findByPublicId(String publicId);

    @Query("select b from RoomBlock b join fetch b.room where b.deletedAt is null order by b.startDate")
    List<RoomBlock> findAllActive();
}
