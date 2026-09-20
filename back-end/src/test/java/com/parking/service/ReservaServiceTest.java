package com.parking.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Clock;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ReservaServiceTest {

    @Test
    void generarCodigoReservaDebeSerUnicoEnSecuencia() throws Exception {
        ReservaService service = new ReservaService(
                null,
                null,
                null,
                null,
                null,
                null,
                Clock.systemDefaultZone()
        );

        Method method = ReservaService.class.getDeclaredMethod("generarCodigoReserva");
        method.setAccessible(true);

        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            String codigo = (String) method.invoke(service);
            assertTrue(codigos.add(codigo), "Se generó un código de reserva duplicado: " + codigo);
        }
    }
}
