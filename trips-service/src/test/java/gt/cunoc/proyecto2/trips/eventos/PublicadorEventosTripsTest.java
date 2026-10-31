package gt.cunoc.proyecto2.trips.eventos;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.dtos.rutas.RutaRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.NombresViaje;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.eventos.publicados.alquileres.AlquilerAsignado;
import gt.cunoc.proyecto2.trips.eventos.publicados.alquileres.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.trips.eventos.publicados.rutas.RutaEliminada;
import gt.cunoc.proyecto2.trips.eventos.publicados.rutas.RutaEvento;
import gt.cunoc.proyecto2.trips.eventos.publicados.viajes.ViajeCompletado;
import gt.cunoc.proyecto2.trips.eventos.publicados.viajes.ViajeEliminado;
import gt.cunoc.proyecto2.trips.eventos.publicados.viajes.ViajeEvento;
import gt.cunoc.proyecto2.trips.eventos.publicados.viajes.ViajeIniciado;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.SentidoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicadorEventosTripsTest {

    /** 25-oct 21:00 y 23:30 en Guatemala, escritos en UTC (ya es 26-oct en UTC). */
    private static final OffsetDateTime NOCHE_UTC = OffsetDateTime.parse("2026-10-26T03:00:00Z");
    private static final OffsetDateTime MEDIANOCHE_UTC = OffsetDateTime.parse("2026-10-26T05:30:00Z");
    private static final LocalDate DIA_VIAJE = LocalDate.of(2026, 10, 25);

    private final PublicadorEventos publicador = mock(PublicadorEventos.class);
    private final PublicadorEventosTrips eventos = new PublicadorEventosTrips(publicador);
    private final RutaEntidad ruta = DatosPrueba.ruta();

    private static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/Guatemala"));
    }

    /** Verifica tipo de agregado, id, version, nombre del evento y businessDate; devuelve el payload. */
    private Object publicado(String agregado, UUID id, Long version, String evento, LocalDate fecha) {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(publicador).publicar(eq(agregado), eq(id), eq(version), eq(evento), payload.capture(), eq(fecha));
        return payload.getValue();
    }

    private ViajeRespuesta respuesta(ViajeEntidad viaje) {
        return new ViajeRespuesta(viaje, new NombresViaje(Map.of(ruta.getId(), ruta), Map.of(),
                Map.of(XELA, "Xela", GUATE, "Guate"),
                Map.of(viaje.getBusId(), DatosPrueba.vistaBus(viaje.getBusId(), "P-123ABC")),
                Map.of(viaje.getChoferId(), DatosPrueba.vistaChofer(viaje.getChoferId(), "Juan Perez"))));
    }

    /** Viaje regular que sale el 25-oct a las 21:00 de Guatemala (26-oct en UTC). */
    private ViajeEntidad viajeDeNoche() {
        AsignacionViaje asignacion = new AsignacionViaje(UUID.randomUUID(), 45, UUID.randomUUID(),
                DatosPrueba.SALARIO, NOCHE_UTC, MEDIANOCHE_UTC);
        return DatosPrueba.viajeRegular(ruta, asignacion);
    }

    // ----- Rutas -----

    @Test
    void rutaCreadaYActualizadaConFechaDeHoy() {
        RutaRespuesta creada = new RutaRespuesta(ruta, Map.of(XELA, "Xela", GUATE, "Guate"));

        eventos.rutaCreada(creada);
        Object payloadCreada = publicado("Ruta", ruta.getId(), 0L, "RutaCreada", hoy());

        DatosPrueba.conVersion(ruta, 1);
        ruta.actualizar(new BigDecimal("210.00"), new BigDecimal("80.00"), false);
        RutaRespuesta actualizada = new RutaRespuesta(ruta, Map.of(XELA, "Xela", GUATE, "Guate"));
        eventos.rutaActualizada(actualizada);
        Object payloadActualizada = publicado("Ruta", ruta.getId(), 1L, "RutaActualizada", hoy());

        assertThat(payloadCreada).isEqualTo(RutaEvento.desde(creada));
        assertThat(payloadActualizada).isInstanceOfSatisfying(RutaEvento.class, e -> {
            assertThat(e.rutaId()).isEqualTo(ruta.getId());
            assertThat(e.origenNombre()).isEqualTo("Xela");
            assertThat(e.destinoNombre()).isEqualTo("Guate");
            assertThat(e.precioBoleto()).isEqualByComparingTo("80.00");
            assertThat(e.activa()).isFalse();
        });
    }

    @Test
    void rutaEliminadaPublicaLaVersionSiguiente() {
        DatosPrueba.conVersion(ruta, 3);

        eventos.rutaEliminada(ruta);

        Object payload = publicado("Ruta", ruta.getId(), 4L, "RutaEliminada", hoy());
        assertThat(payload).isEqualTo(new RutaEliminada(ruta.getId()));
    }

    @Test
    void rutaEliminadaSinVersionPublicaVersionNula() {
        RutaEntidad nueva = RutaEntidad.registrar(XELA, GUATE, new BigDecimal("205.00"), DatosPrueba.PRECIO_BOLETO,
                UUID.randomUUID());

        eventos.rutaEliminada(nueva);

        publicado("Ruta", nueva.getId(), null, "RutaEliminada", hoy());
    }

    // ----- Viajes -----

    @Test
    void viajeProgramadoConElDiaDeSalidaEnGuatemala() {
        ViajeEntidad viaje = viajeDeNoche();
        ViajeRespuesta programado = respuesta(viaje);

        eventos.viajeProgramado(programado);

        Object payload = publicado("Viaje", viaje.getId(), 0L, "ViajeProgramado", DIA_VIAJE);
        assertThat(payload).isEqualTo(ViajeEvento.desde(programado));
        assertThat(payload).isInstanceOfSatisfying(ViajeEvento.class, e -> {
            assertThat(e.tipo()).isEqualTo(TipoViaje.REGULAR);
            assertThat(e.busPlaca()).isEqualTo("P-123ABC");
            assertThat(e.fechaSalida()).isAtSameInstantAs(NOCHE_UTC);
        });
    }

    @Test
    void viajeActualizadoConSuVersionYElDiaDeSalida() {
        ViajeEntidad viaje = DatosPrueba.conVersion(DatosPrueba.viajeRegular(ruta, DatosPrueba.asignacion()), 2);
        ViajeRespuesta actualizado = respuesta(viaje);

        eventos.viajeActualizado(actualizado);

        Object payload = publicado("Viaje", viaje.getId(), 2L, "ViajeActualizado", DIA_VIAJE);
        assertThat(payload).isInstanceOfSatisfying(ViajeEvento.class, e -> {
            assertThat(e.viajeId()).isEqualTo(viaje.getId());
            assertThat(e.capacidad()).isEqualTo(45);
            assertThat(e.precioBoleto()).isEqualByComparingTo("75.00");
        });
    }

    @Test
    void viajeEliminadoPublicaLaVersionSiguienteYElMotivo() {
        ViajeEntidad viaje = DatosPrueba.conVersion(DatosPrueba.viajeRegular(ruta, DatosPrueba.asignacion()), 1);

        eventos.viajeEliminado(viaje, "Bus en mantenimiento");

        Object payload = publicado("Viaje", viaje.getId(), 2L, "ViajeEliminado", hoy());
        assertThat(payload).isEqualTo(new ViajeEliminado(viaje.getId(), TipoViaje.REGULAR, XELA,
                "Bus en mantenimiento"));
    }

    @Test
    void viajeEliminadoSinVersionPublicaVersionNula() {
        ViajeEntidad viaje = ViajeEntidad.programarRegular(ruta, DatosPrueba.asignacion(), UUID.randomUUID(), SentidoViaje.IDA, false);

        eventos.viajeEliminado(viaje, null);

        Object payload = publicado("Viaje", viaje.getId(), null, "ViajeEliminado", hoy());
        assertThat(payload).isInstanceOfSatisfying(ViajeEliminado.class, e -> assertThat(e.motivo()).isNull());
    }

    @Test
    void viajeIniciadoConElDiaDeLaSalida() {
        ViajeEntidad viaje = DatosPrueba.conVersion(viajeDeNoche(), 1);
        SalidaViajeEntidad salida = SalidaViajeEntidad.registrar(viaje, NOCHE_UTC.plusMinutes(10),
                new BigDecimal("1000.00"), UUID.randomUUID());

        eventos.viajeIniciado(viaje, salida);

        Object payload = publicado("Viaje", viaje.getId(), 1L, "ViajeIniciado", DIA_VIAJE);
        assertThat(payload).isInstanceOfSatisfying(ViajeIniciado.class, e -> {
            assertThat(e.viajeId()).isEqualTo(viaje.getId());
            assertThat(e.busId()).isEqualTo(viaje.getBusId());
            assertThat(e.choferId()).isEqualTo(viaje.getChoferId());
            assertThat(e.kmInicial()).isEqualByComparingTo("1000.00");
            assertThat(e.fechaHoraSalida()).isAtSameInstantAs(NOCHE_UTC.plusMinutes(10));
        });
    }

    @Test
    void viajeCompletadoConElDiaDeLaLlegada() {
        ViajeEntidad viaje = DatosPrueba.conVersion(viajeDeNoche(), 2);
        SalidaViajeEntidad salida = SalidaViajeEntidad.registrar(viaje, NOCHE_UTC, new BigDecimal("1000.00"),
                UUID.randomUUID());
        LlegadaViajeEntidad llegada = LlegadaViajeEntidad.registrar(viaje, salida, MEDIANOCHE_UTC,
                new BigDecimal("1200.00"), new BigDecimal("900.00"), new BigDecimal("1.5000"), UUID.randomUUID());

        eventos.viajeCompletado(viaje, salida, llegada);

        Object payload = publicado("Viaje", viaje.getId(), 2L, "ViajeCompletado", DIA_VIAJE);
        assertThat(payload).isInstanceOfSatisfying(ViajeCompletado.class, e -> {
            assertThat(e.kmRecorridos()).isEqualByComparingTo("200.00");
            assertThat(e.costoDepreciacion()).isEqualByComparingTo("300.00");
            assertThat(e.salarioChofer()).isEqualByComparingTo("350.00");
            assertThat(e.fechaHoraLlegada()).isAtSameInstantAs(MEDIANOCHE_UTC);
        });
    }

    // ----- Alquileres -----

    @Test
    void cotizacionConfirmadaConFechaDeHoy() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        alquiler.cotizar(new BigDecimal("2600.00"), UUID.randomUUID());
        DatosPrueba.conVersion(alquiler, 1);

        eventos.cotizacionAlquilerConfirmada(alquiler);

        Object payload = publicado("AlquilerPrivado", alquiler.getId(), 1L, "CotizacionAlquilerConfirmada", hoy());
        assertThat(payload).isInstanceOfSatisfying(CotizacionAlquilerConfirmada.class, e -> {
            assertThat(e.alquilerId()).isEqualTo(alquiler.getId());
            assertThat(e.canal()).isEqualTo(CanalAlquiler.EN_LINEA);
            assertThat(e.precioConfirmado()).isEqualByComparingTo("2600.00");
            assertThat(e.cantidadPasajeros()).isEqualTo(30);
        });
        verifyNoMoreInteractions(publicador);
    }

    @Test
    void alquilerAsignadoConFechaDeHoy() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();
        DatosPrueba.conVersion(alquiler, 3);
        ViajeRespuesta viaje = respuesta(viajeDeNoche());

        eventos.alquilerAsignado(alquiler, viaje);

        Object payload = publicado("AlquilerPrivado", alquiler.getId(), 3L, "AlquilerAsignado", hoy());
        assertThat(payload).isInstanceOfSatisfying(AlquilerAsignado.class, e -> {
            assertThat(e.alquilerId()).isEqualTo(alquiler.getId());
            assertThat(e.viajeId()).isEqualTo(viaje.viajeId());
            assertThat(e.clienteCorreo()).isEqualTo("cliente@correo.com");
            assertThat(e.busPlaca()).isEqualTo("P-123ABC");
            assertThat(e.choferNombre()).isEqualTo("Juan Perez");
        });
        verifyNoMoreInteractions(publicador);
    }
}
