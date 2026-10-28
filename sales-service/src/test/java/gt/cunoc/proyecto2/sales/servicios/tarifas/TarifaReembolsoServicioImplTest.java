package gt.cunoc.proyecto2.sales.servicios.tarifas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.dtos.tarifas.ActualizarTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.NuevaTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.ReglaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.dtos.tarifas.TarifaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.sales.errores.tarifas.ErrorTarifaReembolso;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.modelos.tarifas.TarifaReembolsoEntidad;
import gt.cunoc.proyecto2.sales.repositorios.tarifas.TarifaReembolsoRepositorio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TarifaReembolsoServicioImplTest {

    private final TarifaReembolsoRepositorio repositorio = mock(TarifaReembolsoRepositorio.class);
    private final PublicadorEventosSales eventos = mock(PublicadorEventosSales.class);

    private TarifaReembolsoServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new TarifaReembolsoServicioImpl(repositorio, eventos);
        when(repositorio.saveAndFlush(any(TarifaReembolsoEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static TarifaReembolsoEntidad tarifa(EstadoAlquilerTarifa estado, int horas, boolean permite,
                                                 String porcentaje) {
        TarifaReembolsoEntidad tarifa = TarifaReembolsoEntidad.crear(estado, horas, permite,
                new BigDecimal(porcentaje), "Tramo de " + horas + " horas");
        ReflectionTestUtils.setField(tarifa, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(tarifa, "version", 0);
        return tarifa;
    }

    private void laTablaTiene(TarifaReembolsoEntidad... tarifas) {
        when(repositorio.findByActivaTrueAndEstadoAlquilerIn(any())).thenReturn(List.of(tarifas));
    }

    // ----- Resolución -----

    @Test
    void ganaElTramoMasAltoQueLaCancelacionAlcanza() {
        laTablaTiene(tarifa(EstadoAlquilerTarifa.ASIGNADO, 24, true, "80.00"),
                tarifa(EstadoAlquilerTarifa.ASIGNADO, 1, true, "60.00"),
                tarifa(EstadoAlquilerTarifa.ASIGNADO, 0, false, "0.00"));

        ReglaReembolsoRespuesta regla = servicio.aplicable(EstadoAlquilerTarifa.ASIGNADO, 30);

        assertThat(regla.permiteCancelacion()).isTrue();
        assertThat(regla.porcentajeReembolso()).isEqualByComparingTo("80.00");
    }

    @Test
    void conMenosDeVeinticuatroHorasCaeAlTramoDelSesentaPorCiento() {
        laTablaTiene(tarifa(EstadoAlquilerTarifa.ASIGNADO, 24, true, "80.00"),
                tarifa(EstadoAlquilerTarifa.ASIGNADO, 1, true, "60.00"),
                tarifa(EstadoAlquilerTarifa.ASIGNADO, 0, false, "0.00"));

        assertThat(servicio.aplicable(EstadoAlquilerTarifa.ASIGNADO, 5).porcentajeReembolso())
                .isEqualByComparingTo("60.00");
    }

    @Test
    void aLaHoraDeLaSalidaYaNoSePermiteCancelar() {
        laTablaTiene(tarifa(EstadoAlquilerTarifa.ASIGNADO, 24, true, "80.00"),
                tarifa(EstadoAlquilerTarifa.ASIGNADO, 1, true, "60.00"),
                tarifa(EstadoAlquilerTarifa.ASIGNADO, 0, false, "0.00"));

        ReglaReembolsoRespuesta regla = servicio.aplicable(EstadoAlquilerTarifa.ASIGNADO, 0);

        assertThat(regla.permiteCancelacion()).isFalse();
        assertThat(regla.porcentajeReembolso()).isEqualByComparingTo("0.00");
    }

    /** Si el viaje ya salió, las horas son negativas y aplica igual el tramo de 0 horas. */
    @Test
    void conHorasNegativasAplicaElTramoDeCero() {
        laTablaTiene(tarifa(EstadoAlquilerTarifa.ASIGNADO, 0, false, "0.00"));

        assertThat(servicio.aplicable(EstadoAlquilerTarifa.ASIGNADO, -40).permiteCancelacion()).isFalse();
    }

    @Test
    void elEstadoEspecificoManadaSobreElComodinEnElMismoTramo() {
        laTablaTiene(tarifa(EstadoAlquilerTarifa.CUALQUIERA, 0, true, "50.00"),
                tarifa(EstadoAlquilerTarifa.PAGADO, 0, true, "95.00"));

        assertThat(servicio.aplicable(EstadoAlquilerTarifa.PAGADO, 100).porcentajeReembolso())
                .isEqualByComparingTo("95.00");
    }

    @Test
    void sinNingunaTarifaAplicableSePermiteCancelarConDevolucionTotal() {
        laTablaTiene();

        ReglaReembolsoRespuesta regla = servicio.aplicable(EstadoAlquilerTarifa.PAGADO, 10);

        assertThat(regla.tarifaId()).isNull();
        assertThat(regla.permiteCancelacion()).isTrue();
        assertThat(regla.porcentajeReembolso()).isEqualByComparingTo("100.00");
    }

    @Test
    void sinEstadoSeBuscaSoloElComodin() {
        laTablaTiene(tarifa(EstadoAlquilerTarifa.CUALQUIERA, 0, true, "70.00"));

        assertThat(servicio.aplicable(null, 10).porcentajeReembolso()).isEqualByComparingTo("70.00");
    }

    @Test
    void elReembolsoSeCalculaSobreElMontoPagado() {
        laTablaTiene(tarifa(EstadoAlquilerTarifa.ASIGNADO, 0, true, "60.00"));

        assertThat(servicio.aplicable(EstadoAlquilerTarifa.ASIGNADO, 3)
                .reembolsoDe(new BigDecimal("2600.00"))).isEqualByComparingTo("1560.00");
    }

    // ----- CRUD -----

    @Test
    void crearPublicaElCambioParaQueTripsActualiceSuCopia() {
        when(repositorio.findByEstadoAlquilerAndHorasMinimasAntes(any(), anyInt())).thenReturn(Optional.empty());

        TarifaReembolsoRespuesta respuesta = servicio.crear(new NuevaTarifaReembolsoSolicitud(
                EstadoAlquilerTarifa.PAGADO, 0, true, new BigDecimal("95.00"), "  Se retiene el 5%  "));

        assertThat(respuesta.porcentajeReembolso()).isEqualByComparingTo("95.00");
        assertThat(respuesta.justificacion()).isEqualTo("Se retiene el 5%");
        assertThat(respuesta.activa()).isTrue();
        verify(eventos).tarifaReembolsoCambiada(any(TarifaReembolsoEntidad.class));
    }

    @Test
    void noSePuedenCrearDosTarifasParaElMismoTramo() {
        TarifaReembolsoEntidad existente = tarifa(EstadoAlquilerTarifa.PAGADO, 24, true, "90.00");
        when(repositorio.findByEstadoAlquilerAndHorasMinimasAntes(EstadoAlquilerTarifa.PAGADO, 24))
                .thenReturn(Optional.of(existente));

        VerificarError.lanza(() -> servicio.crear(new NuevaTarifaReembolsoSolicitud(
                        EstadoAlquilerTarifa.PAGADO, 24, true, new BigDecimal("80.00"), "otra")),
                ErrorTarifaReembolso.TRAMO_DUPLICADO);
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void siNoSePermiteCancelarElPorcentajeQuedaEnCero() {
        when(repositorio.findByEstadoAlquilerAndHorasMinimasAntes(any(), anyInt())).thenReturn(Optional.empty());

        TarifaReembolsoRespuesta respuesta = servicio.crear(new NuevaTarifaReembolsoSolicitud(
                EstadoAlquilerTarifa.ASIGNADO, 0, false, new BigDecimal("50.00"), "Fuera de plazo"));

        assertThat(respuesta.permiteCancelacion()).isFalse();
        assertThat(respuesta.porcentajeReembolso()).isEqualByComparingTo("0.00");
    }

    @Test
    void actualizarCambiaElPorcentajeYPuedeDesactivarLaTarifa() {
        TarifaReembolsoEntidad existente = tarifa(EstadoAlquilerTarifa.ASIGNADO, 24, true, "80.00");
        when(repositorio.findById(existente.getId())).thenReturn(Optional.of(existente));

        TarifaReembolsoRespuesta respuesta = servicio.actualizar(existente.getId(),
                new ActualizarTarifaReembolsoSolicitud(true, new BigDecimal("70.00"), "Nuevo texto", false, 0));

        assertThat(respuesta.porcentajeReembolso()).isEqualByComparingTo("70.00");
        assertThat(respuesta.justificacion()).isEqualTo("Nuevo texto");
        assertThat(respuesta.activa()).isFalse();
        assertThat(respuesta.horasMinimasAntes()).isEqualTo(24);        // el tramo no se edita
        verify(eventos).tarifaReembolsoCambiada(existente);
    }

    @Test
    void actualizarConVersionViejaEsConflicto() {
        TarifaReembolsoEntidad existente = tarifa(EstadoAlquilerTarifa.ASIGNADO, 24, true, "80.00");
        when(repositorio.findById(existente.getId())).thenReturn(Optional.of(existente));

        VerificarError.lanza(() -> servicio.actualizar(existente.getId(),
                        new ActualizarTarifaReembolsoSolicitud(true, new BigDecimal("70.00"), "x", true, 5)),
                ErrorGeneral.MODIFICACION_CONCURRENTE);
    }

    @Test
    void actualizarUnaTarifaQueNoExisteEsNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.actualizar(id,
                        new ActualizarTarifaReembolsoSolicitud(true, new BigDecimal("70.00"), "x", true, 0)),
                ErrorTarifaReembolso.TARIFA_NO_EXISTE);
    }

    @Test
    void listarDevuelveTodasIncluidasLasDesactivadas() {
        TarifaReembolsoEntidad activa = tarifa(EstadoAlquilerTarifa.PAGADO, 0, true, "95.00");
        TarifaReembolsoEntidad inactiva = tarifa(EstadoAlquilerTarifa.ASIGNADO, 24, true, "80.00");
        inactiva.desactivar();
        when(repositorio.findAllByOrderByEstadoAlquilerAscHorasMinimasAntesDesc())
                .thenReturn(List.of(activa, inactiva));

        assertThat(servicio.listar()).extracting(TarifaReembolsoRespuesta::activa)
                .containsExactly(true, false);
    }

    @Test
    void reactivarVuelveAPonerLaTarifaEnJuego() {
        TarifaReembolsoEntidad existente = tarifa(EstadoAlquilerTarifa.ASIGNADO, 24, true, "80.00");
        existente.desactivar();
        when(repositorio.findById(existente.getId())).thenReturn(Optional.of(existente));

        TarifaReembolsoRespuesta respuesta = servicio.actualizar(existente.getId(),
                new ActualizarTarifaReembolsoSolicitud(true, new BigDecimal("80.00"), "Igual", true, 0));

        assertThat(respuesta.activa()).isTrue();
    }

    /** Una tarifa desactivada no cubre nada, aunque el tramo coincida. */
    @Test
    void unaTarifaDesactivadaNoCubre() {
        TarifaReembolsoEntidad inactiva = tarifa(EstadoAlquilerTarifa.ASIGNADO, 0, true, "80.00");
        inactiva.desactivar();

        assertThat(inactiva.cubre(EstadoAlquilerTarifa.ASIGNADO, 100)).isFalse();
    }

    @Test
    void elNombreDeUnEstadoDesconocidoCaeAlComodin() {
        assertThat(EstadoAlquilerTarifa.deNombre("asignado")).isEqualTo(EstadoAlquilerTarifa.ASIGNADO);
        assertThat(EstadoAlquilerTarifa.deNombre("  PAGADO ")).isEqualTo(EstadoAlquilerTarifa.PAGADO);
        assertThat(EstadoAlquilerTarifa.deNombre("INVENTADO")).isEqualTo(EstadoAlquilerTarifa.CUALQUIERA);
        assertThat(EstadoAlquilerTarifa.deNombre(null)).isEqualTo(EstadoAlquilerTarifa.CUALQUIERA);
        assertThat(EstadoAlquilerTarifa.deNombre("  ")).isEqualTo(EstadoAlquilerTarifa.CUALQUIERA);
    }
}
