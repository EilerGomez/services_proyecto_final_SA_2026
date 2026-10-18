package gt.cunoc.proyecto2.wallet.models;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Callbacks JPA en el mismo paquete (son package-private).
class CarteraTest {

    @Test
    void alCrearFijaFechaYAlActualizarLaRefresca() throws Exception {
        // Arrange
        Cartera cartera = new Cartera();
        cartera.setSaldo(BigDecimal.ZERO);
        Thread.sleep(5);

        // Act
        cartera.alCrear();
        var primera = cartera.getFechaActualizacion();
        Thread.sleep(5);
        cartera.alActualizar();

        // Assert
        assertAll(
                () -> assertNotNull(primera),
                () -> assertTrue(cartera.getFechaActualizacion().isAfter(primera)));
    }

    @Test
    void alCrearRespetaFechaExistente() {
        // Arrange
        Cartera cartera = new Cartera();
        var fija = Instant.parse("2026-01-01T00:00:00Z");
        cartera.setFechaActualizacion(fija);

        // Act
        cartera.alCrear();

        // Assert
        assertEquals(fija, cartera.getFechaActualizacion());
    }
}
