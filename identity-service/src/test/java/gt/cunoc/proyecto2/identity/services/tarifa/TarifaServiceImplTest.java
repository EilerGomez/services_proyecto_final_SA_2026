package gt.cunoc.proyecto2.identity.services.tarifa;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.identity.dtos.tarifa.CreateDepreciationRateRequest;
import gt.cunoc.proyecto2.identity.messaging.publisher.TarifaEventPublisher;
import gt.cunoc.proyecto2.identity.models.TarifaDepreciacion;
import gt.cunoc.proyecto2.identity.repositories.TarifaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarifaServiceImplTest {

    @Mock private TarifaRepository tarifas;
    @Mock private TarifaEventPublisher publicador;

    @InjectMocks
    private TarifaServiceImpl servicio;

    @Test
    void crearPublicaConFechaVigente() {
        // Arrange
        var solicitud = new CreateDepreciationRateRequest(
                new BigDecimal("1.2500"), LocalDate.of(2026, 10, 10));

        // Act
        var respuesta = servicio.crear(solicitud, UUID.randomUUID());

        // Assert
        assertAll(
                () -> assertEquals(0, new BigDecimal("1.2500").compareTo(respuesta.montoPorKm())),
                () -> verify(tarifas).saveAndFlush(any(TarifaDepreciacion.class)),
                () -> verify(publicador).publicarTarifaDepreciacionCambiada(any(), any(), any(), any()));
    }

    @Test
    void crearFechaRepetidaDaConflicto() {
        // Arrange
        var solicitud = new CreateDepreciationRateRequest(
                new BigDecimal("1.2500"), LocalDate.of(2026, 10, 10));
        when(tarifas.saveAndFlush(any(TarifaDepreciacion.class)))
                .thenThrow(new DataIntegrityViolationException("duplicada"));

        // Act
        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.crear(solicitud, UUID.randomUUID()));

        // Assert
        assertEquals("TARIFA_YA_EXISTE_PARA_FECHA", error.getCodigo());
    }

    @Test
    void vigenteSinTarifaDa404() {
        // Arrange
        when(tarifas.findTopByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.empty());

        // Act
        var error = assertThrows(NoEncontradoException.class,
                () -> servicio.vigente(LocalDate.of(2026, 1, 1)));

        // Assert
        assertEquals("TARIFA_NO_EXISTE", error.getCodigo());
    }

    @Test
    void listarDevuelveHistorial() {
        // Arrange
        TarifaDepreciacion tarifa = new TarifaDepreciacion();
        tarifa.setMontoPorKm(new BigDecimal("1.1000"));
        when(tarifas.findAllByOrderByVigenteDesdeDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tarifa)));

        // Act
        var pagina = servicio.listar(Pageable.unpaged());

        // Assert
        assertEquals(1, pagina.getTotalElements());
    }
}
