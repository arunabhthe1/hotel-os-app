package com.hotelos.web.dto;

import com.hotelos.domain.Booking;
import com.hotelos.domain.Order;
import com.hotelos.domain.Payment;
import com.hotelos.domain.RoomBlock;
import com.hotelos.domain.enums.BookingSource;
import com.hotelos.domain.enums.BookingStatus;
import com.hotelos.domain.enums.OrderStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record CheckoutRequest(
            @NotBlank @Size(max = 120) String guestName,
            @NotBlank @Email String guestEmail,
            @NotBlank @Size(max = 32) String guestPhone
    ) {
    }

    public record ConfirmPaymentRequest(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {
    }

    public record CheckoutResponse(
            String orderId,
            OrderStatus status,
            BigDecimal totalAmount,
            String currency,
            String razorpayOrderId,
            String razorpayKeyId,
            boolean simulated
    ) {
    }

    public record WalkInRequest(
            @NotBlank String roomId,
            @NotBlank @Size(max = 120) String guestName,
            @Email String guestEmail,
            @NotBlank @Size(max = 32) String guestPhone,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Size(max = 500) String notes
    ) {
    }

    public record ModifyBookingRequest(
            String roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            @Size(max = 120) String guestName,
            @Size(max = 32) String guestPhone,
            @Size(max = 500) String notes
    ) {
    }

    public record BlockRequest(
            @NotBlank String roomId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotBlank @Size(max = 255) String reason
    ) {
    }

    public record BookingResponse(
            String id,
            String orderId,
            String roomId,
            String roomNumber,
            String roomName,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal pricePerNight,
            BigDecimal totalAmount,
            BookingStatus status,
            BookingSource source,
            String paymentId,
            String notes,
            Instant createdAt
    ) {
        public static BookingResponse from(Booking booking, Payment payment) {
            return new BookingResponse(
                    booking.getPublicId(),
                    booking.getOrder().getPublicId(),
                    booking.getRoom().getPublicId(),
                    booking.getRoom().getRoomNumber(),
                    booking.getRoom().getName(),
                    booking.getGuestName(),
                    booking.getGuestEmail(),
                    booking.getGuestPhone(),
                    booking.getCheckIn(),
                    booking.getCheckOut(),
                    booking.getNights(),
                    booking.getPricePerNight(),
                    booking.getTotalAmount(),
                    booking.getStatus(),
                    booking.getSource(),
                    payment == null ? null : payment.getPublicId(),
                    booking.getNotes(),
                    booking.getCreatedAt()
            );
        }

        public static BookingResponse from(Booking booking) {
            return from(booking, null);
        }
    }

    public record OrderResponse(
            String id,
            OrderStatus status,
            BookingSource source,
            BigDecimal totalAmount,
            String currency
    ) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.getPublicId(),
                    order.getStatus(),
                    order.getSource(),
                    order.getTotalAmount(),
                    order.getCurrency()
            );
        }
    }

    public record RoomBlockResponse(
            String id,
            String roomId,
            String roomNumber,
            String roomName,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            Instant createdAt
    ) {
        public static RoomBlockResponse from(RoomBlock block) {
            return new RoomBlockResponse(
                    block.getPublicId(),
                    block.getRoom().getPublicId(),
                    block.getRoom().getRoomNumber(),
                    block.getRoom().getName(),
                    block.getStartDate(),
                    block.getEndDate(),
                    block.getReason(),
                    block.getCreatedAt()
            );
        }
    }
}
