package com.hotelos.service;

import com.hotelos.domain.Room;
import com.hotelos.domain.RoomMonthSetting;
import com.hotelos.repository.RoomMonthSettingRepository;
import com.hotelos.util.StayDates;
import com.hotelos.web.advice.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final RoomMonthSettingRepository monthSettingRepository;

    @Transactional(readOnly = true)
    public StayQuote quote(Room room, LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> nights = StayDates.stayNights(checkIn, checkOut);
        Map<YearMonth, RoomMonthSetting> settings = settingsFor(room.getId(), nights);

        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate night : nights) {
            RoomMonthSetting setting = settings.get(YearMonth.from(night));
            if (setting != null && !setting.isAvailable()) {
                return StayQuote.unavailable(room.getBasePrice(), nights.size());
            }
            total = total.add(effectivePrice(room, setting));
        }

        BigDecimal perNight = total.divide(BigDecimal.valueOf(nights.size()), 2, RoundingMode.HALF_UP);
        return new StayQuote(nights.size(), perNight, total, false);
    }

    @Transactional(readOnly = true)
    public boolean monthsOpen(Room room, List<LocalDate> nights) {
        Map<YearMonth, RoomMonthSetting> settings = settingsFor(room.getId(), nights);
        return nights.stream().noneMatch(night -> {
            RoomMonthSetting setting = settings.get(YearMonth.from(night));
            return setting != null && !setting.isAvailable();
        });
    }

    public BigDecimal effectivePrice(Room room, RoomMonthSetting setting) {
        if (setting != null && setting.getPriceOverride() != null) {
            return setting.getPriceOverride();
        }
        return room.getBasePrice();
    }

    public Map<YearMonth, RoomMonthSetting> settingsFor(Long roomId, Collection<LocalDate> nights) {
        if (nights.isEmpty()) {
            return Map.of();
        }
        LocalDate min = nights.stream().min(LocalDate::compareTo).orElseThrow();
        LocalDate max = nights.stream().max(LocalDate::compareTo).orElseThrow();
        List<RoomMonthSetting> rows = monthSettingRepository.findByRoomIdInAndMonthStartBetween(
                List.of(roomId),
                YearMonth.from(min).atDay(1),
                YearMonth.from(max).atDay(1)
        );
        Map<YearMonth, RoomMonthSetting> byMonth = new HashMap<>();
        for (RoomMonthSetting row : rows) {
            byMonth.put(YearMonth.from(row.getMonthStart()), row);
        }
        return byMonth;
    }

    public Map<Long, Map<YearMonth, RoomMonthSetting>> settingsIndexed(
            Collection<Long> roomIds,
            LocalDate fromMonthStart,
            LocalDate toMonthStart
    ) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }
        List<RoomMonthSetting> rows = monthSettingRepository.findByRoomIdInAndMonthStartBetween(
                roomIds,
                fromMonthStart,
                toMonthStart
        );
        Map<Long, Map<YearMonth, RoomMonthSetting>> indexed = new HashMap<>();
        for (RoomMonthSetting row : rows) {
            indexed.computeIfAbsent(row.getRoom().getId(), ignored -> new HashMap<>())
                    .put(YearMonth.from(row.getMonthStart()), row);
        }
        return indexed;
    }

    public record StayQuote(int nights, BigDecimal pricePerNight, BigDecimal total, boolean unavailable) {
        public static StayQuote unavailable(BigDecimal basePrice, int nights) {
            return new StayQuote(nights, basePrice, BigDecimal.ZERO, true);
        }

        public void requireAvailable() {
            if (unavailable || nights <= 0) {
                throw ApiException.badRequest("Invalid stay dates or pricing unavailable");
            }
        }
    }
}
