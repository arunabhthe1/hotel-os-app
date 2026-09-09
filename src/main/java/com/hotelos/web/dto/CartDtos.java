package com.hotelos.web.dto;

import com.hotelos.domain.CartItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddCartItemRequest(
            @NotBlank String roomId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut
    ) {
    }

    public record CartItemResponse(
            String roomId,
            String roomNumber,
            String roomName,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal pricePerNight,
            BigDecimal totalAmount
    ) {
        public static CartItemResponse from(CartItem item) {
            return new CartItemResponse(
                    item.getRoom().getPublicId(),
                    item.getRoom().getRoomNumber(),
                    item.getRoom().getName(),
                    item.getCheckIn(),
                    item.getCheckOut(),
                    item.getNights(),
                    item.getPricePerNight(),
                    item.getTotalAmount()
            );
        }
    }

    public record CartResponse(List<CartItemResponse> items, BigDecimal totalAmount) {
        public static CartResponse of(List<CartItemResponse> items) {
            BigDecimal total = items.stream()
                    .map(CartItemResponse::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CartResponse(items, total);
        }
    }
}
