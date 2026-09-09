package com.hotelos.service;

import com.hotelos.domain.Room;
import com.hotelos.domain.RoomBlock;
import com.hotelos.domain.User;
import com.hotelos.domain.enums.OccupancySourceType;
import com.hotelos.repository.RoomBlockRepository;
import com.hotelos.util.PublicIds;
import com.hotelos.util.StayDates;
import com.hotelos.web.advice.ApiException;
import com.hotelos.web.dto.BookingDtos.BlockRequest;
import com.hotelos.web.dto.BookingDtos.RoomBlockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomBlockService {

    private final RoomBlockRepository roomBlockRepository;
    private final RoomQueryService roomQueryService;
    private final OccupancyService occupancyService;

    @Transactional(readOnly = true)
    public List<RoomBlockResponse> listActive() {
        return roomBlockRepository.findAllActive().stream()
                .map(RoomBlockResponse::from)
                .toList();
    }

    @Transactional
    public RoomBlockResponse create(User manager, BlockRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("Invalid block date range");
        }

        Room room = occupancyService.lockRoom(roomQueryService.requireByPublicId(request.roomId()).getId());
        List<LocalDate> nights = StayDates.inclusiveNights(request.startDate(), request.endDate());
        occupancyService.requireFree(room.getId(), nights);

        RoomBlock block = new RoomBlock();
        block.setPublicId(PublicIds.next());
        block.setRoom(room);
        block.setStartDate(request.startDate());
        block.setEndDate(request.endDate());
        block.setReason(request.reason().trim());
        block.setCreatedBy(manager);
        roomBlockRepository.save(block);
        occupancyService.occupy(room, OccupancySourceType.block, block.getId(), nights);
        return RoomBlockResponse.from(block);
    }

    @Transactional
    public void remove(String publicId) {
        RoomBlock block = roomBlockRepository.findByPublicId(publicId)
                .orElseThrow(() -> ApiException.notFound("Room block not found"));
        if (block.getDeletedAt() != null) {
            return;
        }
        occupancyService.release(OccupancySourceType.block, block.getId());
        block.setDeletedAt(Instant.now());
        roomBlockRepository.save(block);
    }
}
