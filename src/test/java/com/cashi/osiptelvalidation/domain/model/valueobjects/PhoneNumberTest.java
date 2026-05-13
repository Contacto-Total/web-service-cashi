package com.cashi.osiptelvalidation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberTest {

    @Test
    void aceptaMovilPeruanoNormalizado() {
        assertEquals("987654321", PhoneNumber.of("987654321").value());
    }

    @Test
    void normalizaCodigoPaisExplicito() {
        assertEquals("987654321", PhoneNumber.of("+51987654321").value());
        assertEquals("987654321", PhoneNumber.of("51987654321").value());
    }

    @Test
    void normalizaEspaciosYGuiones() {
        assertEquals("987654321", PhoneNumber.of(" 987-654-321 ").value());
        assertEquals("987654321", PhoneNumber.of("(987) 654 321").value());
    }

    @Test
    void rechazaLineaFija() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of("1234567"));
    }

    @Test
    void rechazaMovilQueNoEmpiezaEn9() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of("887654321"));
    }

    @Test
    void rechazaLargoIncorrecto() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of("98765432"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of("9876543210"));
    }

    @Test
    void rechazaNull() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of(null));
    }

    @Test
    void mascaraOcultaPrefijo() {
        assertEquals("******321", PhoneNumber.of("987654321").masked());
    }

    @Test
    void igualdadPorValor() {
        assertEquals(PhoneNumber.of("987654321"), PhoneNumber.of("+51987654321"));
        assertEquals(PhoneNumber.of("987654321").hashCode(), PhoneNumber.of("987654321").hashCode());
    }

    @Test
    void isValidNoLanzaExcepcion() {
        assertTrue(PhoneNumber.isValid("987654321"));
        assertFalse(PhoneNumber.isValid("123"));
        assertFalse(PhoneNumber.isValid(null));
    }
}
