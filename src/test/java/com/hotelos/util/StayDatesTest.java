package com.hotelos.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StayDatesTest {

    @Test
    void stayNightsAreCheckoutExclusive() {
        List<LocalDate> nights = StayDates.stayNights(LocalDate.parse("2026-08-10"), LocalDate.parse("2026-08-12"));
        assertEquals(List.of(LocalDate.parse("2026-08-10"), LocalDate.parse("2026-08-11")), nights);
    }

    @Test
    void blockNightsAreInclusive() {
        List<LocalDate> nights = StayDates.inclusiveNights(LocalDate.parse("2026-08-12"), LocalDate.parse("2026-08-14"));
        assertEquals(3, nights.size());
        assertEquals(LocalDate.parse("2026-08-14"), nights.get(2));
    }

    @Test
    void monthStartMustBeFirstDay() {
        assertEquals(LocalDate.parse("2026-09-01"), StayDates.monthStart("2026-09"));
        assertThrows(IllegalArgumentException.class, () -> StayDates.monthStart("2026/09"));
    }
}
