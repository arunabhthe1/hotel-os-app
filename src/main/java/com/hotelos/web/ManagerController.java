package com.hotelos.web;

import com.hotelos.security.CurrentUserService;
import com.hotelos.service.BookingService;
import com.hotelos.service.RoomBlockService;
import com.hotelos.web.dto.BookingDtos.BlockRequest;
import com.hotelos.web.dto.BookingDtos.BookingResponse;
import com.hotelos.web.dto.BookingDtos.ModifyBookingRequest;
import com.hotelos.web.dto.BookingDtos.RoomBlockResponse;
import com.hotelos.web.dto.BookingDtos.WalkInRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final CurrentUserService currentUserService;
    private final BookingService bookingService;
    private final RoomBlockService roomBlockService;

    @GetMapping("/bookings")
    public List<BookingResponse> bookings() {
        return bookingService.forManager();
    }

    @PatchMapping("/bookings/{bookingId}")
    public BookingResponse modify(
            @PathVariable String bookingId,
            @Valid @RequestBody ModifyBookingRequest request
    ) {
        return bookingService.modify(currentUserService.entity(), bookingId, request);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public BookingResponse cancel(@PathVariable String bookingId) {
        return bookingService.cancel(currentUserService.entity(), bookingId);
    }

    @PostMapping("/walk-ins")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse walkIn(@Valid @RequestBody WalkInRequest request) {
        return bookingService.createWalkIn(currentUserService.entity(), request);
    }

    @GetMapping("/blocks")
    public List<RoomBlockResponse> blocks() {
        return roomBlockService.listActive();
    }

    @PostMapping("/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomBlockResponse createBlock(@Valid @RequestBody BlockRequest request) {
        return roomBlockService.create(currentUserService.entity(), request);
    }

    @DeleteMapping("/blocks/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBlock(@PathVariable String blockId) {
        roomBlockService.remove(blockId);
    }
}
