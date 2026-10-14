package gt.cunoc.proyecto2.identity.services.tarifa;

import gt.cunoc.proyecto2.identity.models.TarifaDepreciacion;
import gt.cunoc.proyecto2.identity.messaging.publisher.TarifaEventPublisher;
import gt.cunoc.proyecto2.identity.repositories.TarifaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ramas de TarifaServiceImpl sin cubrir: fecha nula (usa hoy) y vigente con fecha explícita. */
@ExtendWith(MockitoExtension.class)
class TarifaServiceRamasTest {

    @Mock private TarifaRepository tarifas;
    @Mock private TarifaEventPublisher publicador;

    @InjectMocks
    private TarifaServiceImpl servicio;

    @Test
    void vigenteNulaUsaHoy() {
        // Arrange
        TarifaDepreciacion tarifa = tarifa();
        when(tarifas.findTopByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(eq(LocalDate.now())))
                .thenReturn(Optional.of(tarifa));

        // Act
        var respuesta = servicio.vigente(null);

        // Assert
        assertAll(
                () -> assertEquals(0, new BigDecimal("1.2500").compareTo(respuesta.montoPorKm())),
                () -> verify(tarifas)
                        .findTopByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(any(LocalDate.class)));
    }

    @Test
    void vigenteConFechaDevuelveTarifa() {
        // Arrange
        TarifaDepreciacion tarifa = tarifa();
        when(tarifas.findTopByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                LocalDate.of(2026, 10, 20))).thenReturn(Optional.of(tarifa));

        // Act
        var respuesta = servicio.vigente(LocalDate.of(2026, 10, 20));

        // Assert
        assertEquals(0, new BigDecimal("1.2500").compareTo(respuesta.montoPorKm()));
    }

    private TarifaDepreciacion tarifa() {
        TarifaDepreciacion tarifa = new TarifaDepreciacion();
        tarifa.setMontoPorKm(new BigDecimal("1.2500"));
        tarifa.setVigenteDesde(LocalDate.of(2026, 10, 10));
        return tarifa;
    }
}
