package com.hotelos.web;

import com.hotelos.domain.enums.RoomType;
import com.hotelos.service.RoomQueryService;
import com.hotelos.web.dto.RoomDtos.AvailableRoomResponse;
import com.hotelos.web.dto.RoomDtos.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomQueryService roomQueryService;

    @GetMapping
    public List<RoomResponse> list() {
        return roomQueryService.listActive();
    }

    @GetMapping("/{publicId}")
    public RoomResponse get(@PathVariable String publicId) {
        return RoomResponse.from(roomQueryService.requireByPublicId(publicId));
    }

    @GetMapping("/availability")
    public List<AvailableRoomResponse> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) RoomType type
    ) {
        return roomQueryService.search(checkIn, checkOut, type);
    }
}
