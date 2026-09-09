package com.hotelos.web.dto;

import com.hotelos.domain.Room;
import com.hotelos.domain.enums.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RoomDtos {

    private RoomDtos() {
    }

    public record RoomResponse(
            String id,
            String number,
            String name,
            RoomType type,
            String description,
            int capacity,
            List<String> amenities,
            String image,
            BigDecimal basePrice
    ) {
        public static RoomResponse from(Room room) {
            List<String> amenities = room.getAmenities().stream()
                    .map(amenity -> amenity.getLabel())
                    .sorted()
                    .toList();
            return new RoomResponse(
                    room.getPublicId(),
                    room.getRoomNumber(),
                    room.getName(),
                    room.getRoomType(),
                    room.getDescription(),
                    room.getCapacity(),
                    amenities,
                    room.getImageUrl(),
                    room.getBasePrice()
            );
        }
    }

    public record StayQuote(
            int nights,
            BigDecimal pricePerNight,
            BigDecimal total,
            boolean unavailable
    ) {
    }

    public record AvailableRoomResponse(
            RoomResponse room,
            boolean available,
            StayQuote quote
    ) {
    }

    public record MonthSettingResponse(
            String month,
            LocalDate monthStart,
            boolean available,
            BigDecimal priceOverride,
            BigDecimal effectivePrice
    ) {
    }

    public record InventoryRoomResponse(
            RoomResponse room,
            List<MonthSettingResponse> months
    ) {
    }

    public record UpdateBasePriceRequest(
            @NotNull @DecimalMin("0.01") BigDecimal basePrice
    ) {
    }

    public record UpdateMonthSettingRequest(
            Boolean available,
            @DecimalMin("0.01") BigDecimal priceOverride
    ) {
    }

    public record BulkMonthAvailabilityRequest(
            @NotNull Boolean available
    ) {
    }
}
