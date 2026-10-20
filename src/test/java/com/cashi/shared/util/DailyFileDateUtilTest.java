package com.cashi.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyFileDateUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Cartera_Castigo_2026_10_17.xlsx",
            "Cartera_INNOVAG_2026-10-17.xlsx",
            "Cartera_20261017.xlsx"
    })
    void extraeLaFechaDelNombre(String fileName) {
        assertEquals(Optional.of("2026_10_17"), DailyFileDateUtil.extractArchiveDate(fileName));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Cartera.xlsx",
            "Cartera_2026_13_40.xlsx",
            "Cartera_2026_10-17.xlsx"
    })
    void rechazaNombresSinFechaValida(String fileName) {
        assertTrue(DailyFileDateUtil.extractArchiveDate(fileName).isEmpty());
    }

    @Test
    void rechazaNombreVacio() {
        assertTrue(DailyFileDateUtil.extractArchiveDate(null).isEmpty());
        assertTrue(DailyFileDateUtil.extractArchiveDate("  ").isEmpty());
    }
}
