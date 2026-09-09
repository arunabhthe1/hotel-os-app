package com.hotelos.service;

import com.hotelos.domain.Room;
import com.hotelos.domain.enums.RoomType;
import com.hotelos.repository.RoomRepository;
import com.hotelos.util.StayDates;
import com.hotelos.web.advice.ApiException;
import com.hotelos.web.dto.RoomDtos.AvailableRoomResponse;
import com.hotelos.web.dto.RoomDtos.RoomResponse;
import com.hotelos.web.dto.RoomDtos.StayQuote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomQueryService {

    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final OccupancyService occupancyService;

    @Transactional(readOnly = true)
    public List<RoomResponse> listActive() {
        return roomRepository.findAllActiveWithAmenities().stream()
                .map(RoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Room requireByPublicId(String publicId) {
        return roomRepository.findByPublicIdWithAmenities(publicId)
                .orElseThrow(() -> ApiException.notFound("Room not found"));
    }

    @Transactional(readOnly = true)
    public List<AvailableRoomResponse> search(LocalDate checkIn, LocalDate checkOut, RoomType type) {
        List<LocalDate> nights;
        try {
            nights = StayDates.stayNights(checkIn, checkOut);
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(ex.getMessage());
        }

        return roomRepository.findAllActiveWithAmenities().stream()
                .filter(room -> type == null || room.getRoomType() == type)
                .map(room -> toAvailability(room, checkIn, checkOut, nights))
                .toList();
    }

    public boolean isBookable(Room room, LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> nights = StayDates.stayNights(checkIn, checkOut);
        if (!room.isActive() || !pricingService.monthsOpen(room, nights)) {
            return false;
        }
        PricingService.StayQuote quote = pricingService.quote(room, checkIn, checkOut);
        if (quote.unavailable()) {
            return false;
        }
        return occupancyService.isFree(room.getId(), nights);
    }

    private AvailableRoomResponse toAvailability(
            Room room,
            LocalDate checkIn,
            LocalDate checkOut,
            List<LocalDate> nights
    ) {
        PricingService.StayQuote quote = pricingService.quote(room, checkIn, checkOut);
        boolean free = occupancyService.isFree(room.getId(), nights);
        boolean available = !quote.unavailable() && free;
        return new AvailableRoomResponse(
                RoomResponse.from(room),
                available,
                new StayQuote(quote.nights(), quote.pricePerNight(), quote.total(), quote.unavailable())
        );
    }
}
