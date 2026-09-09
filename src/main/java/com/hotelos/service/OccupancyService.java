package com.hotelos.service;

import com.hotelos.domain.Room;
import com.hotelos.domain.RoomNightOccupancy;
import com.hotelos.domain.enums.OccupancySourceType;
import com.hotelos.repository.RoomNightOccupancyRepository;
import com.hotelos.repository.RoomRepository;
import com.hotelos.web.advice.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OccupancyService {

    private final RoomRepository roomRepository;
    private final RoomNightOccupancyRepository occupancyRepository;

    @Transactional
    public Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> ApiException.notFound("Room not found"));
    }

    @Transactional(readOnly = true)
    public boolean isFree(Long roomId, List<LocalDate> nights) {
        if (nights.isEmpty()) {
            return false;
        }
        return occupancyRepository.findOccupiedDates(roomId, nights).isEmpty();
    }

    public void requireFree(Long roomId, List<LocalDate> nights) {
        List<LocalDate> occupied = occupancyRepository.findOccupiedDates(roomId, nights);
        if (!occupied.isEmpty()) {
            throw ApiException.conflict("Room is not available for the selected dates");
        }
    }

    @Transactional
    public void occupy(Room room, OccupancySourceType sourceType, Long sourceId, List<LocalDate> nights) {
        requireFree(room.getId(), nights);
        try {
            for (LocalDate night : nights) {
                RoomNightOccupancy occupancy = new RoomNightOccupancy();
                occupancy.setRoom(room);
                occupancy.setStayDate(night);
                occupancy.setSourceType(sourceType);
                occupancy.setSourceId(sourceId);
                occupancyRepository.save(occupancy);
            }
            occupancyRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Room is not available for the selected dates");
        }
    }

    @Transactional
    public void release(OccupancySourceType sourceType, Long sourceId) {
        occupancyRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
    }
}
