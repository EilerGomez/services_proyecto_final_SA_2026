package gt.cunoc.proyecto2.trips.eventos.publicados.viajes;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.dtos.viajes.NombresViaje;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Fábricas de los payloads de viaje: ViajeEvento, ViajeEliminado, ViajeIniciado y ViajeCompletado. */
class PayloadsViajeTest {

    private static final ZoneOffset GUATEMALA = ZoneOffset.ofHours(-6);

    private final RutaEntidad ruta = DatosPrueba.ruta();
    private final ViajeEntidad regular = DatosPrueba.viajeRegular(ruta, DatosPrueba.asignacion());

    @Test
    void viajeEventoRegularLlevaRutaPrecioYNombres() {
        ViajeRespuesta respuesta = new ViajeRespuesta(regular, new NombresViaje(Map.of(ruta.getId(), ruta), Map.of(),
                Map.of(XELA, "Xela", GUATE, "Guate"), Map.of(regular.getBusId(), "P-123ABC"),
                Map.of(regular.getChoferId(), "Juan Perez")));

        ViajeEvento evento = ViajeEvento.desde(respuesta);

        assertThat(evento.viajeId()).isEqualTo(regular.getId());
        assertThat(evento.tipo()).isEqualTo(TipoViaje.REGULAR);
        assertThat(evento.rutaId()).isEqualTo(ruta.getId());
        assertThat(evento.alquilerId()).isNull();
        assertThat(evento.sucursalId()).isEqualTo(XELA);
        assertThat(evento.origenNombre()).isEqualTo("Xela");
        assertThat(evento.destinoNombre()).isEqualTo("Guate");
        assertThat(evento.busId()).isEqualTo(regular.getBusId());
        assertThat(evento.busPlaca()).isEqualTo("P-123ABC");
        assertThat(evento.choferId()).isEqualTo(regular.getChoferId());
        assertThat(evento.capacidad()).isEqualTo(45);
        assertThat(evento.precioBoleto()).isEqualByComparingTo("75.00");
        assertThat(evento.fechaSalida()).isEqualTo(respuesta.fechaSalidaProgramada());
        assertThat(evento.fechaLlegadaEstimada()).isEqualTo(respuesta.fechaLlegadaEstimada());
        assertThat(evento.fechaSalida().getOffset()).isEqualTo(GUATEMALA);
    }

    @Test
    void viajeEventoPrivadoLlevaAlquilerSinRutaNiPrecio() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();
        ViajeEntidad privado = DatosPrueba.viajePrivado(alquiler, DatosPrueba.asignacion());
        ViajeRespuesta respuesta = new ViajeRespuesta(privado, new NombresViaje(Map.of(),
                Map.of(alquiler.getId(), alquiler), Map.of(), Map.of(), Map.of()));

        ViajeEvento evento = ViajeEvento.desde(respuesta);

        assertThat(evento.tipo()).isEqualTo(TipoViaje.PRIVADO);
        assertThat(evento.rutaId()).isNull();
        assertThat(evento.alquilerId()).isEqualTo(alquiler.getId());
        assertThat(evento.precioBoleto()).isNull();
        assertThat(evento.origenNombre()).isEqualTo("Quetzaltenango");
        assertThat(evento.destinoNombre()).isEqualTo("Antigua Guatemala");
        assertThat(evento.busPlaca()).isNull();
    }

    @Test
    void viajeEliminadoLlevaTipoSucursalYMotivo() {
        ViajeEliminado evento = ViajeEliminado.desde(regular, "Bus en mantenimiento");

        assertThat(evento).isEqualTo(new ViajeEliminado(regular.getId(), TipoViaje.REGULAR, XELA,
                "Bus en mantenimiento"));
    }

    @Test
    void viajeIniciadoUsaElBusYChoferDeLaSalida() {
        SalidaViajeEntidad salida = SalidaViajeEntidad.registrar(regular,
                OffsetDateTime.parse("2026-10-25T12:05:00Z"), new BigDecimal("1000.00"), UUID.randomUUID());
        UUID busSalida = regular.getBusId();
        UUID choferSalida = regular.getChoferId();
        regular.reprogramar(DatosPrueba.asignacion(UUID.randomUUID(), UUID.randomUUID(), 50));

        ViajeIniciado evento = ViajeIniciado.desde(regular, salida);

        assertThat(evento.viajeId()).isEqualTo(regular.getId());
        assertThat(evento.tipo()).isEqualTo(TipoViaje.REGULAR);
        assertThat(evento.busId()).isEqualTo(busSalida).isNotEqualTo(regular.getBusId());
        assertThat(evento.choferId()).isEqualTo(choferSalida);
        assertThat(evento.kmInicial()).isEqualByComparingTo("1000.00");
        assertThat(evento.fechaHoraSalida()).isEqualTo(OffsetDateTime.parse("2026-10-25T06:05:00-06:00"));
    }

    @Test
    void viajeCompletadoRegularLlevaLosCostosCalculados() {
        SalidaViajeEntidad salida = DatosPrueba.salida(regular);
        LlegadaViajeEntidad llegada = DatosPrueba.llegada(regular, salida);

        ViajeCompletado evento = ViajeCompletado.desde(regular, salida, llegada);

        assertThat(evento.viajeId()).isEqualTo(regular.getId());
        assertThat(evento.tipo()).isEqualTo(TipoViaje.REGULAR);
        assertThat(evento.rutaId()).isEqualTo(ruta.getId());
        assertThat(evento.alquilerId()).isNull();
        assertThat(evento.sucursalId()).isEqualTo(XELA);
        assertThat(evento.busId()).isEqualTo(salida.getBusId());
        assertThat(evento.choferId()).isEqualTo(salida.getChoferId());
        assertThat(evento.fechaHoraLlegada()).isAtSameInstantAs(DatosPrueba.LLEGADA.plusMinutes(10));
        assertThat(evento.kmInicial()).isEqualByComparingTo("1000.00");
        assertThat(evento.kmFinal()).isEqualByComparingTo("1207.50");
        assertThat(evento.kmRecorridos()).isEqualByComparingTo("207.50");
        assertThat(evento.costoCombustible()).isEqualByComparingTo("950.00");
        assertThat(evento.tarifaDepreciacion()).isEqualByComparingTo("1.2500");
        assertThat(evento.costoDepreciacion()).isEqualByComparingTo("259.38");
        assertThat(evento.salarioChofer()).isEqualByComparingTo("350.00");
    }

    @Test
    void viajeCompletadoPrivadoLlevaElAlquilerYElRecargoDelChofer() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();
        AsignacionViaje asignacion = DatosPrueba.asignacion();
        ViajeEntidad privado = DatosPrueba.viajePrivado(alquiler, asignacion);
        SalidaViajeEntidad salida = DatosPrueba.salida(privado);
        LlegadaViajeEntidad llegada = DatosPrueba.llegada(privado, salida);

        ViajeCompletado evento = ViajeCompletado.desde(privado, salida, llegada);

        assertThat(evento.tipo()).isEqualTo(TipoViaje.PRIVADO);
        assertThat(evento.rutaId()).isNull();
        assertThat(evento.alquilerId()).isEqualTo(alquiler.getId());
        assertThat(evento.salarioChofer()).isEqualByComparingTo("402.50");
    }
}
