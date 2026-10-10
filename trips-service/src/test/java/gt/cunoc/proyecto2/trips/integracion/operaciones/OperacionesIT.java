package gt.cunoc.proyecto2.trips.integracion.operaciones;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.trips.integracion.soporte.PruebaIntegracion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** T14 (salida) y T15 (llegada con costos): registros inmutables y eventos ViajeIniciado / ViajeCompletado. */
class OperacionesIT extends PruebaIntegracion {

    private static final OffsetDateTime HORA_SALIDA = SALIDA.plusMinutes(10);
    private static final OffsetDateTime HORA_LLEGADA = OffsetDateTime.parse("2026-11-20T10:30:00-06:00");

    private UUID xela;
    private UUID guate;
    private UUID ruta;
    private UUID bus;
    private UUID chofer;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("Quetzaltenango", true);
        guate = insertarSucursal("Guatemala", true);
        ruta = insertarRuta(xela, guate, true);
        bus = insertarBus(xela, "P-111AAA");
        chofer = insertarChofer(xela, "Juan Perez");
        insertarTarifa("0.5000", LocalDate.of(2026, 1, 1));
        insertarTarifa("0.7500", LocalDate.of(2026, 12, 1));          // posterior a la llegada: no se usa
    }

    private static String salida(OffsetDateTime fechaHora, String kmInicial) {
        return "{\"fechaHoraSalida\":\"%s\",\"kmInicial\":%s}".formatted(fechaHora, kmInicial);
    }

    private static String llegada(OffsetDateTime fechaHora, String kmFinal, String combustible) {
        return "{\"fechaHoraLlegada\":\"%s\",\"kmFinal\":%s,\"costoCombustible\":%s}"
                .formatted(fechaHora, kmFinal, combustible);
    }

    private static String rutaSalida(UUID viajeId) {
        return "/trips/viajes/" + viajeId + "/salida";
    }

    private static String rutaLlegada(UUID viajeId) {
        return "/trips/viajes/" + viajeId + "/llegada";
    }

    /** Viaje EN_RUTA con su salida (km 1000.00 a las 06:10). */
    private UUID viajeEnRuta() {
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "EN_RUTA");
        insertarSalida(viaje, bus, chofer, HORA_SALIDA, "1000.00");
        return viaje;
    }

    // ----- T14 -----

    @Test
    void elChoferRegistraLaSalidaYSePublicaViajeIniciado() {
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");
        UUID usuarioChofer = UUID.randomUUID();

        ResponseEntity<String> respuesta = post(rutaSalida(viaje), token(usuarioChofer, Roles.CHOFER, null, chofer),
                salida(HORA_SALIDA, "1000.00"));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("EN_RUTA");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isEqualTo(1);
        assertThat(decimal(respuesta, "$.salida.kmInicial")).isEqualByComparingTo("1000.00");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.salida.registradoPor"))
                .isEqualTo(usuarioChofer.toString());
        assertThat(fecha(respuesta, "$.salida.fechaHoraSalida")).isEqualTo(HORA_SALIDA);
        assertThat(estadoDelViaje(viaje)).isEqualTo("EN_RUTA");
        assertThat(jdbc.queryForObject("SELECT bus_id FROM salidas_viaje WHERE viaje_id = ?", UUID.class, viaje))
                .isEqualTo(bus);

        String evento = esperarEventos("ViajeIniciado").get("ViajeIniciado");
        assertThat(JsonPath.<String>read(evento, "$.aggregateId")).isEqualTo(viaje.toString());
        assertThat(versionDelEvento(evento)).isEqualTo(1);
        assertThat(JsonPath.<String>read(evento, "$.businessDate")).isEqualTo(DIA.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.busId")).isEqualTo(bus.toString());
        assertThat(decimal(evento, "$.payload.kmInicial")).isEqualByComparingTo("1000.00");
        assertThat(fecha(evento, "$.payload.fechaHoraSalida")).isEqualTo(HORA_SALIDA);
    }

    @Test
    void elAdminDeLaSucursalTambienRegistraLaSalida() {
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");

        esStatus(post(rutaSalida(viaje), tokenAdminSucursal(xela), salida(HORA_SALIDA, "0")), 200);
        assertThat(contar("salidas_viaje")).isEqualTo(1);
    }

    @Test
    void unaSalidaYaRegistradaEs409() {
        UUID viaje = viajeEnRuta();

        esError(post(rutaSalida(viaje), tokenChofer(chofer), salida(HORA_SALIDA, "1000.00")),
                409, "SALIDA_YA_REGISTRADA");
        assertThat(contar("salidas_viaje")).isEqualTo(1);
    }

    @Test
    void soloElChoferAsignadoOElAdminDeLaSucursalOperan() {
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");

        esError(post(rutaSalida(viaje), tokenChofer(UUID.randomUUID()), salida(HORA_SALIDA, "1000.00")),
                403, "CHOFER_NO_ASIGNADO");
        esError(post(rutaSalida(viaje), tokenAdminSucursal(guate), salida(HORA_SALIDA, "1000.00")),
                403, "SUCURSAL_AJENA");
        esError(post(rutaSalida(viaje), tokenCajero(xela), salida(HORA_SALIDA, "1000.00")),
                403, "ROL_INSUFICIENTE");
        esError(post(rutaSalida(UUID.randomUUID()), tokenChofer(chofer), salida(HORA_SALIDA, "1000.00")),
                404, "VIAJE_NO_EXISTE");
        assertThat(estadoDelViaje(viaje)).isEqualTo("PROGRAMADO");
    }

    @Test
    void unaSalidaInvalidaEs400() {
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");

        esError(post(rutaSalida(viaje), tokenChofer(chofer), salida(HORA_SALIDA, "-5")), 400, "VALIDACION");
        esError(post(rutaSalida(viaje), tokenChofer(chofer), "{\"kmInicial\":10}"), 400, "VALIDACION");
    }

    // ----- T15 -----

    @Test
    void laLlegadaCalculaLosCostosYPublicaViajeCompletado() {
        UUID viaje = viajeEnRuta();

        ResponseEntity<String> respuesta = post(rutaLlegada(viaje), tokenChofer(chofer),
                llegada(HORA_LLEGADA, "1250.50", "800.00"));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("COMPLETADO");
        assertThat(decimal(respuesta, "$.salida.kmInicial")).isEqualByComparingTo("1000.00");
        assertThat(decimal(respuesta, "$.llegada.kmRecorridos")).isEqualByComparingTo("250.50");
        assertThat(decimal(respuesta, "$.llegada.tarifaDepreciacion")).isEqualByComparingTo("0.5");
        assertThat(decimal(respuesta, "$.llegada.costoDepreciacion")).isEqualByComparingTo("125.25");
        assertThat(decimal(respuesta, "$.llegada.costoCombustible")).isEqualByComparingTo("800.00");
        assertThat(decimal(respuesta, "$.llegada.salarioChofer")).isEqualByComparingTo("350.00");
        assertThat(fecha(respuesta, "$.llegada.fechaHoraLlegada")).isEqualTo(HORA_LLEGADA);
        assertThat(jdbc.queryForObject("SELECT costo_depreciacion FROM llegadas_viaje WHERE viaje_id = ?",
                BigDecimal.class, viaje)).isEqualByComparingTo("125.25");

        String evento = esperarEventos("ViajeCompletado").get("ViajeCompletado");
        assertThat(versionDelEvento(evento)).isEqualTo(1);
        assertThat(JsonPath.<String>read(evento, "$.businessDate")).isEqualTo(DIA.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.rutaId")).isEqualTo(ruta.toString());
        assertThat(decimal(evento, "$.payload.kmFinal")).isEqualByComparingTo("1250.50");
        assertThat(decimal(evento, "$.payload.costoDepreciacion")).isEqualByComparingTo("125.25");
        assertThat(decimal(evento, "$.payload.salarioChofer")).isEqualByComparingTo("350.00");
    }

    @Test
    void laLlegadaDeUnViajePrivadoPaga15PorCientoMasYCompletaElAlquiler() {
        UUID alquiler = insertarAlquiler(xela, UUID.randomUUID(), "ASIGNADO");
        UUID viaje = insertarViajePrivado(alquiler, xela, bus, chofer, SALIDA, "EN_RUTA");
        insertarSalida(viaje, bus, chofer, HORA_SALIDA, "1000.00");

        ResponseEntity<String> respuesta = post(rutaLlegada(viaje), tokenAdminSucursal(xela),
                llegada(HORA_LLEGADA, "1100.00", "300.00"));

        esStatus(respuesta, 200);
        assertThat(decimal(respuesta, "$.llegada.salarioChofer")).isEqualByComparingTo("402.50");   // 350 × 1.15
        assertThat(decimal(respuesta, "$.llegada.costoDepreciacion")).isEqualByComparingTo("50.00");
        assertThat(estadoDelAlquiler(alquiler)).isEqualTo("COMPLETADO");
        String evento = esperarEventos("ViajeCompletado").get("ViajeCompletado");
        assertThat(JsonPath.<String>read(evento, "$.payload.tipo")).isEqualTo("PRIVADO");
        assertThat(JsonPath.<String>read(evento, "$.payload.alquilerId")).isEqualTo(alquiler.toString());
    }

    @Test
    void unViajeQueNoHaSalidoOQueYaLlegoNoRegistraLlegada() {
        UUID programado = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");
        UUID completado = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA.minusDays(1), "COMPLETADO");

        esError(post(rutaLlegada(programado), tokenChofer(chofer), llegada(HORA_LLEGADA, "1100.00", "1")),
                422, "VIAJE_NO_INICIADO");
        esError(post(rutaLlegada(completado), tokenChofer(chofer), llegada(HORA_LLEGADA, "1100.00", "1")),
                409, "LLEGADA_YA_REGISTRADA");
    }

    @Test
    void kilometrajeMenorOLlegadaAntesDeLaSalidaFallan() {
        UUID viaje = viajeEnRuta();

        esError(post(rutaLlegada(viaje), tokenChofer(chofer), llegada(HORA_LLEGADA, "999.99", "1")),
                422, "KM_INVALIDO");
        esError(post(rutaLlegada(viaje), tokenChofer(chofer), llegada(HORA_SALIDA, "1100.00", "1")),
                400, "LLEGADA_ANTES_DE_SALIDA");
        // los errores revierten la transacción: el viaje sigue EN_RUTA y sin llegada
        assertThat(estadoDelViaje(viaje)).isEqualTo("EN_RUTA");
        assertThat(contar("llegadas_viaje")).isZero();
    }

    @Test
    void sinTarifaVigenteElDiaDeLlegadaEs422() {
        jdbc.execute("TRUNCATE TABLE vista_tarifas_depreciacion");
        insertarTarifa("0.5000", LocalDate.of(2026, 12, 1));
        UUID viaje = viajeEnRuta();

        esError(post(rutaLlegada(viaje), tokenChofer(chofer), llegada(HORA_LLEGADA, "1100.00", "1")),
                422, "SIN_TARIFA_VIGENTE");
        assertThat(estadoDelViaje(viaje)).isEqualTo("EN_RUTA");
    }

    @Test
    void otroChoferNoRegistraLaLlegada() {
        UUID viaje = viajeEnRuta();

        esError(post(rutaLlegada(viaje), tokenChofer(UUID.randomUUID()), llegada(HORA_LLEGADA, "1100.00", "1")),
                403, "CHOFER_NO_ASIGNADO");
        esError(post(rutaLlegada(viaje), tokenChofer(chofer), "{\"kmFinal\":1100}"), 400, "VALIDACION");
    }
}
