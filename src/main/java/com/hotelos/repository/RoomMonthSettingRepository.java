package com.hotelos.repository;

import com.hotelos.domain.Room;
import com.hotelos.domain.RoomMonthSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoomMonthSettingRepository extends JpaRepository<RoomMonthSetting, Long> {

    Optional<RoomMonthSetting> findByRoomAndMonthStart(Room room, LocalDate monthStart);

    List<RoomMonthSetting> findByRoomIdInAndMonthStartBetween(Collection<Long> roomIds, LocalDate from, LocalDate to);

    List<RoomMonthSetting> findByMonthStart(LocalDate monthStart);
}
