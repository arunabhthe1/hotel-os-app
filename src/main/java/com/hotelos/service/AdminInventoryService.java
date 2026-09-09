package com.hotelos.service;

import com.hotelos.domain.Room;
import com.hotelos.domain.RoomMonthSetting;
import com.hotelos.domain.User;
import com.hotelos.repository.RoomMonthSettingRepository;
import com.hotelos.repository.RoomRepository;
import com.hotelos.util.StayDates;
import com.hotelos.web.advice.ApiException;
import com.hotelos.web.dto.RoomDtos.InventoryRoomResponse;
import com.hotelos.web.dto.RoomDtos.MonthSettingResponse;
import com.hotelos.web.dto.RoomDtos.RoomResponse;
import com.hotelos.web.dto.RoomDtos.UpdateMonthSettingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminInventoryService {

    private final RoomRepository roomRepository;
    private final RoomMonthSettingRepository monthSettingRepository;
    private final RoomQueryService roomQueryService;
    private final PricingService pricingService;

    @Transactional(readOnly = true)
    public List<InventoryRoomResponse> inventory(int monthsAhead) {
        int months = Math.max(1, Math.min(monthsAhead, 24));
        YearMonth start = YearMonth.now();
        YearMonth end = start.plusMonths(months - 1L);
        List<Room> rooms = roomRepository.findAllWithAmenities();
        Map<Long, Map<YearMonth, RoomMonthSetting>> settings = pricingService.settingsIndexed(
                rooms.stream().map(Room::getId).toList(),
                start.atDay(1),
                end.atDay(1)
        );

        List<InventoryRoomResponse> result = new ArrayList<>();
        for (Room room : rooms) {
            Map<YearMonth, RoomMonthSetting> byMonth = settings.getOrDefault(room.getId(), Map.of());
            List<MonthSettingResponse> monthRows = new ArrayList<>();
            for (int i = 0; i < months; i++) {
                YearMonth month = start.plusMonths(i);
                RoomMonthSetting setting = byMonth.get(month);
                boolean available = setting == null || setting.isAvailable();
                BigDecimal override = setting == null ? null : setting.getPriceOverride();
                monthRows.add(new MonthSettingResponse(
                        month.toString(),
                        month.atDay(1),
                        available,
                        override,
                        pricingService.effectivePrice(room, setting)
                ));
            }
            result.add(new InventoryRoomResponse(RoomResponse.from(room), monthRows));
        }
        return result;
    }

    @Transactional
    public RoomResponse updateBasePrice(User admin, String roomPublicId, BigDecimal basePrice) {
        if (basePrice == null || basePrice.signum() <= 0) {
            throw ApiException.badRequest("Enter a valid base price");
        }
        Room room = roomQueryService.requireByPublicId(roomPublicId);
        room.setBasePrice(basePrice);
        roomRepository.save(room);
        return RoomResponse.from(room);
    }

    @Transactional
    public MonthSettingResponse updateMonth(
            User admin,
            String roomPublicId,
            String yearMonth,
            UpdateMonthSettingRequest request
    ) {
        Room room = roomQueryService.requireByPublicId(roomPublicId);
        LocalDate monthStart = StayDates.monthStart(yearMonth);
        RoomMonthSetting setting = monthSettingRepository.findByRoomAndMonthStart(room, monthStart)
                .orElseGet(() -> {
                    RoomMonthSetting created = new RoomMonthSetting();
                    created.setRoom(room);
                    created.setMonthStart(monthStart);
                    created.setAvailable(true);
                    return created;
                });

        if (request.available() != null) {
            setting.setAvailable(request.available());
        }
        if (request.priceOverride() != null) {
            if (request.priceOverride().signum() <= 0) {
                throw ApiException.badRequest("Enter a valid monthly price");
            }
            setting.setPriceOverride(request.priceOverride());
        }
        setting.setUpdatedBy(admin);
        monthSettingRepository.save(setting);

        return new MonthSettingResponse(
                yearMonth,
                monthStart,
                setting.isAvailable(),
                setting.getPriceOverride(),
                pricingService.effectivePrice(room, setting)
        );
    }

    @Transactional
    public void setAllAvailability(User admin, String yearMonth, boolean available) {
        LocalDate monthStart = StayDates.monthStart(yearMonth);
        for (Room room : roomRepository.findAll()) {
            RoomMonthSetting setting = monthSettingRepository.findByRoomAndMonthStart(room, monthStart)
                    .orElseGet(() -> {
                        RoomMonthSetting created = new RoomMonthSetting();
                        created.setRoom(room);
                        created.setMonthStart(monthStart);
                        created.setAvailable(true);
                        return created;
                    });
            setting.setAvailable(available);
            setting.setUpdatedBy(admin);
            monthSettingRepository.save(setting);
        }
    }
}
