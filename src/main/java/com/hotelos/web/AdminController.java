package com.hotelos.web;

import com.hotelos.security.CurrentUserService;
import com.hotelos.service.AdminInventoryService;
import com.hotelos.web.dto.RoomDtos.BulkMonthAvailabilityRequest;
import com.hotelos.web.dto.RoomDtos.InventoryRoomResponse;
import com.hotelos.web.dto.RoomDtos.MonthSettingResponse;
import com.hotelos.web.dto.RoomDtos.RoomResponse;
import com.hotelos.web.dto.RoomDtos.UpdateBasePriceRequest;
import com.hotelos.web.dto.RoomDtos.UpdateMonthSettingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CurrentUserService currentUserService;
    private final AdminInventoryService adminInventoryService;

    @GetMapping("/inventory")
    public List<InventoryRoomResponse> inventory(@RequestParam(defaultValue = "12") int months) {
        return adminInventoryService.inventory(months);
    }

    @PatchMapping("/rooms/{roomId}/base-price")
    public RoomResponse updateBasePrice(
            @PathVariable String roomId,
            @Valid @RequestBody UpdateBasePriceRequest request
    ) {
        return adminInventoryService.updateBasePrice(currentUserService.entity(), roomId, request.basePrice());
    }

    @PutMapping("/rooms/{roomId}/months/{month}")
    public MonthSettingResponse updateMonth(
            @PathVariable String roomId,
            @PathVariable String month,
            @Valid @RequestBody UpdateMonthSettingRequest request
    ) {
        return adminInventoryService.updateMonth(currentUserService.entity(), roomId, month, request);
    }

    @PutMapping("/months/{month}/availability")
    public void setMonthAvailability(
            @PathVariable String month,
            @Valid @RequestBody BulkMonthAvailabilityRequest request
    ) {
        adminInventoryService.setAllAvailability(currentUserService.entity(), month, request.available());
    }
}
