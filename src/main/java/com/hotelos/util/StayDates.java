package com.hotelos.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class StayDates {

    private StayDates() {
    }

    public static List<LocalDate> stayNights(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
        List<LocalDate> nights = new ArrayList<>();
        for (LocalDate day = checkIn; day.isBefore(checkOut); day = day.plusDays(1)) {
            nights.add(day);
        }
        return nights;
    }

    public static List<LocalDate> inclusiveNights(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Block end date must be on or after start date");
        }
        List<LocalDate> nights = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            nights.add(day);
        }
        return nights;
    }

    public static LocalDate monthStart(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth).atDay(1);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Month must be YYYY-MM");
        }
    }

    public static String monthKey(LocalDate date) {
        return YearMonth.from(date).toString();
    }
}
