package gt.cunoc.proyecto2.trips.modelos.viajes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ViajeEntidadTest {

    @Test
    void programarRegularCopiaLaRutaYLaAsignacion() {
        RutaEntidad ruta = DatosPrueba.ruta();
        UUID busId = UUID.randomUUID();
        UUID choferId = UUID.randomUUID();
        AsignacionViaje asignacion = DatosPrueba.asignacion(busId, choferId, 45);
        UUID creadoPor = UUID.randomUUID();

        ViajeEntidad viaje = ViajeEntidad.programarRegular(ruta, asignacion, creadoPor, SentidoViaje.IDA, false);

        assertThat(viaje.getId()).isNotNull();
        assertThat(viaje.getTipo()).isEqualTo(TipoViaje.REGULAR);
        assertThat(viaje.getRutaId()).isEqualTo(ruta.getId());
        assertThat(viaje.getAlquilerId()).isNull();
        assertThat(viaje.getSucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(viaje.getPrecioBoleto()).isEqualByComparingTo("75.00");
        assertThat(viaje.getBusId()).isEqualTo(busId);
        assertThat(viaje.getChoferId()).isEqualTo(choferId);
        assertThat(viaje.getCapacidadBus()).isEqualTo(45);
        assertThat(viaje.getSalarioBaseChofer()).isEqualByComparingTo("350.00");
        assertThat(viaje.getFechaSalidaProgramada()).isEqualTo(DatosPrueba.SALIDA);
        assertThat(viaje.getFechaLlegadaEstimada()).isEqualTo(DatosPrueba.LLEGADA);
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
        assertThat(viaje.getCreadoPor()).isEqualTo(creadoPor);
        assertThat(viaje.getVersion()).isNull();
        assertThat(viaje.esRegular()).isTrue();
        assertThat(viaje.esPrivado()).isFalse();
    }

    @Test
    void programarPrivadoNoTieneRutaNiPrecioDeBoleto() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();

        ViajeEntidad viaje = ViajeEntidad.programarPrivado(alquiler, DatosPrueba.asignacion(), UUID.randomUUID());

        assertThat(viaje.getTipo()).isEqualTo(TipoViaje.PRIVADO);
        assertThat(viaje.getAlquilerId()).isEqualTo(alquiler.getId());
        assertThat(viaje.getRutaId()).isNull();
        assertThat(viaje.getPrecioBoleto()).isNull();
        assertThat(viaje.getSucursalId()).isEqualTo(alquiler.getSucursalId());
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
        assertThat(viaje.esPrivado()).isTrue();
        assertThat(viaje.esRegular()).isFalse();
    }

    @Test
    void reprogramarCambiaBusChoferYHorario() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        UUID busNuevo = UUID.randomUUID();
        UUID choferNuevo = UUID.randomUUID();
        AsignacionViaje nueva = new AsignacionViaje(busNuevo, 50, choferNuevo, new BigDecimal("400.00"),
                DatosPrueba.SALIDA.plusHours(1), DatosPrueba.LLEGADA.plusHours(1));

        viaje.reprogramar(nueva);

        assertThat(viaje.getBusId()).isEqualTo(busNuevo);
        assertThat(viaje.getChoferId()).isEqualTo(choferNuevo);
        assertThat(viaje.getCapacidadBus()).isEqualTo(50);
        assertThat(viaje.getSalarioBaseChofer()).isEqualByComparingTo("400.00");
        assertThat(viaje.getFechaSalidaProgramada()).isEqualTo(DatosPrueba.SALIDA.plusHours(1));
        assertThat(viaje.getFechaLlegadaEstimada()).isEqualTo(DatosPrueba.LLEGADA.plusHours(1));
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
    }

    @Test
    void reprogramarConLaMismaCapacidadSePermite() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();

        assertThatCode(() -> viaje.reprogramar(DatosPrueba.asignacion(UUID.randomUUID(), UUID.randomUUID(), 45)))
                .doesNotThrowAnyException();
    }

    @Test
    void reprogramarConUnBusMasPequenoEsCapacidadMenor() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        UUID busOriginal = viaje.getBusId();

        assertThatThrownBy(() -> viaje.reprogramar(DatosPrueba.asignacion(UUID.randomUUID(), UUID.randomUUID(), 30)))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorViaje.CAPACIDAD_MENOR.codigo());
                    assertThat(e.getMessage()).contains("30 asientos").contains("tiene 45");
                });
        assertThat(viaje.getBusId()).isEqualTo(busOriginal);
    }

    @Test
    void reprogramarUnViajeQueYaSalioEsViajeYaInicio() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        viaje.iniciar();

        VerificarError.lanza(() -> viaje.reprogramar(DatosPrueba.asignacion()), ErrorViaje.VIAJE_YA_INICIO);
    }

    @Test
    void verificarProgramado() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        assertThatCode(viaje::verificarProgramado).doesNotThrowAnyException();

        viaje.iniciar();
        VerificarError.lanza(viaje::verificarProgramado, ErrorViaje.VIAJE_YA_INICIO);
    }

    @Test
    void iniciarPasaAEnRutaYSoloUnaVez() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();

        viaje.iniciar();

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.EN_RUTA);
        VerificarError.lanza(viaje::iniciar, ErrorOperacionViaje.SALIDA_YA_REGISTRADA);
    }

    @Test
    void iniciarUnViajeCompletadoEsSalidaYaRegistrada() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        viaje.iniciar();
        viaje.completar();

        VerificarError.lanza(viaje::iniciar, ErrorOperacionViaje.SALIDA_YA_REGISTRADA);
    }

    @Test
    void completarSinSalidaEsViajeNoIniciado() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();

        VerificarError.lanza(viaje::completar, ErrorOperacionViaje.VIAJE_NO_INICIADO);
        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.PROGRAMADO);
    }

    @Test
    void completarPasaACompletadoYSoloUnaVez() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();
        viaje.iniciar();

        viaje.completar();

        assertThat(viaje.getEstado()).isEqualTo(EstadoViaje.COMPLETADO);
        VerificarError.lanza(viaje::completar, ErrorOperacionViaje.LLEGADA_YA_REGISTRADA);
    }

    @Test
    void salarioDelChoferEnViajeRegularEsElBase() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular();

        assertThat(viaje.salarioChoferPorViaje()).isEqualByComparingTo("350.00");
    }

    @Test
    void salarioDelChoferEnViajePrivadoLlevaQuincePorCiento() {
        ViajeEntidad viaje = DatosPrueba.viajePrivado(DatosPrueba.alquilerPagado(), DatosPrueba.asignacion());

        assertThat(viaje.salarioChoferPorViaje()).isEqualByComparingTo("402.50");
        assertThat(viaje.salarioChoferPorViaje().scale()).isEqualTo(2);
    }

    @Test
    void salarioPrivadoSeRedondeaADosDecimales() {
        AsignacionViaje asignacion = new AsignacionViaje(UUID.randomUUID(), 45, UUID.randomUUID(),
                new BigDecimal("333.33"), DatosPrueba.SALIDA, DatosPrueba.LLEGADA);
        ViajeEntidad viaje = DatosPrueba.viajePrivado(DatosPrueba.alquilerPagado(), asignacion);

        // 333.33 × 1.15 = 383.3295 → 383.33
        assertThat(viaje.salarioChoferPorViaje()).isEqualByComparingTo("383.33");
    }

    @Test
    void motivoNoVendible() {
        ViajeEntidad regular = DatosPrueba.viajeRegular();
        assertThat(regular.motivoNoVendible()).isNull();

        regular.iniciar();
        assertThat(regular.motivoNoVendible()).isEqualTo(MotivoNoVendible.VIAJE_YA_INICIO);

        regular.completar();
        assertThat(regular.motivoNoVendible()).isEqualTo(MotivoNoVendible.VIAJE_YA_INICIO);

        ViajeEntidad privado = DatosPrueba.viajePrivado(DatosPrueba.alquilerPagado(), DatosPrueba.asignacion());
        assertThat(privado.motivoNoVendible()).isEqualTo(MotivoNoVendible.VIAJE_PRIVADO);
    }

    @Test
    void estadosActivosSonProgramadoYEnRuta() {
        assertThat(EstadoViaje.ACTIVOS).containsExactlyInAnyOrder(EstadoViaje.PROGRAMADO, EstadoViaje.EN_RUTA);
        assertThat(EstadoViaje.ACTIVOS).doesNotContain(EstadoViaje.COMPLETADO);
    }

    // ----- Sentido y venta por el piloto (V17) -----

    @Test
    void unViajeNaceDeIdaYSinVentaPorElPiloto() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular(DatosPrueba.ruta(), DatosPrueba.asignacion());

        assertThat(viaje.getSentido()).isEqualTo(SentidoViaje.IDA);
        assertThat(viaje.esRegreso()).isFalse();
        assertThat(viaje.isVentaPorChofer()).isFalse();
    }

    @Test
    void elViajeDeVueltaQuedaComoRegreso() {
        ViajeEntidad vuelta = ViajeEntidad.programarRegular(DatosPrueba.ruta(), DatosPrueba.asignacion(),
                UUID.randomUUID(), SentidoViaje.REGRESO, true);

        assertThat(vuelta.esRegreso()).isTrue();
        assertThat(vuelta.isVentaPorChofer()).isTrue();
    }

    /** Vende el piloto asignado, y solo mientras el viaje no haya salido. */
    @Test
    void soloElPilotoAsignadoVendeYSoloAntesDeSalir() {
        ViajeEntidad viaje = DatosPrueba.viajeRegular(DatosPrueba.ruta(), DatosPrueba.asignacion());

        assertThat(viaje.puedeVenderElChofer(viaje.getChoferId())).isFalse();   // sin la casilla, nadie vende

        viaje.permitirVentaPorChofer(true);

        assertThat(viaje.puedeVenderElChofer(viaje.getChoferId())).isTrue();
        assertThat(viaje.puedeVenderElChofer(UUID.randomUUID())).isFalse();     // otro piloto, no

        viaje.iniciar();

        assertThat(viaje.puedeVenderElChofer(viaje.getChoferId())).isFalse();   // ya salió
    }

    /** Un viaje PRIVADO sale de un alquiler: ni es de regreso ni lo vende el piloto. */
    @Test
    void elViajePrivadoNoEsDeRegresoNiLoVendeElPiloto() {
        ViajeEntidad privado = DatosPrueba.viajePrivado(DatosPrueba.alquilerPagado(), DatosPrueba.asignacion());

        assertThat(privado.getSentido()).isEqualTo(SentidoViaje.IDA);
        assertThat(privado.isVentaPorChofer()).isFalse();
        assertThat(privado.puedeVenderElChofer(privado.getChoferId())).isFalse();
    }
}
