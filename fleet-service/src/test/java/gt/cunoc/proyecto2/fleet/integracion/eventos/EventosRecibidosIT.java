package gt.cunoc.proyecto2.fleet.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.fleet.integracion.soporte.PruebaIntegracion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

/**
 * Eventos que consume Fleet (cola fleet.q): se publican en RabbitMQ como lo haría el relay de
 * Trips o Identity y se espera el efecto en la BD y los eventos que Fleet publica en respuesta.
 */
class EventosRecibidosIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);

    private UUID xela;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("XELA", true);
    }

    private String estadoDelBus(UUID busId) {
        return jdbc.queryForObject("SELECT estado FROM buses WHERE id = ?", String.class, busId);
    }

    private BigDecimal kilometrajeDelBus(UUID busId) {
        return jdbc.queryForObject("SELECT kilometraje_actual FROM buses WHERE id = ?", BigDecimal.class, busId);
    }

    private static String viajeCompletado(UUID viajeId, UUID busId, String kmInicial, String kmFinal) {
        return """
                {"viajeId":"%s","busId":"%s","kmInicial":%s,"kmFinal":%s,
                 "fechaHoraLlegada":"2026-10-20T12:30:00-06:00","otroCampo":"Fleet lo ignora"}
                """.formatted(viajeId, busId, kmInicial, kmFinal);
    }

    // ----- Trips -----

    @Test
    void viajeIniciadoPoneElBusEnRutaYPublicaBusActualizado() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "1000");
        UUID viaje = UUID.randomUUID();

        publicarEvento("trips.eventos", "ViajeIniciado", viaje, """
                {"viajeId":"%s","busId":"%s","fechaHoraSalida":"2026-10-20T08:00:00-06:00"}
                """.formatted(viaje, bus));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoDelBus(bus)).isEqualTo("EN_RUTA"));
        String busActualizado = esperarEventos("BusActualizado").get("BusActualizado");
        assertThat(JsonPath.<String>read(busActualizado, "$.payload.estado")).isEqualTo("EN_RUTA");
        assertThat(JsonPath.<String>read(busActualizado, "$.businessDate")).isEqualTo(FECHA.toString());
    }

    @Test
    void viajeCompletadoRegistraLaBitacoraYSubeElKilometraje() {
        UUID bus = insertarBus(xela, "P-111AAA", "EN_RUTA", "1000");
        UUID viaje = UUID.randomUUID();

        publicarEvento("trips.eventos", "ViajeCompletado", viaje, viajeCompletado(viaje, bus, "1000.00", "1250.50"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(contar("bitacora_kilometraje")).isEqualTo(1));
        assertThat(kilometrajeDelBus(bus)).isEqualByComparingTo("1250.50");
        assertThat(estadoDelBus(bus)).isEqualTo("ACTIVO");
        esperarEventos("BusActualizado");
    }

    @Test
    void unEventoRepetidoSeProcesaUnaSolaVez() {
        UUID bus = insertarBus(xela, "P-111AAA", "EN_RUTA", "1000");
        UUID viaje = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String payload = viajeCompletado(viaje, bus, "1000.00", "1100.00");

        publicarEvento("trips.eventos", "ViajeCompletado", eventId, viaje, payload);
        publicarEvento("trips.eventos", "ViajeCompletado", eventId, viaje, payload);

        await().atMost(ESPERA).untilAsserted(() -> assertThat(contar("bitacora_kilometraje")).isEqualTo(1));
        // durante 2 s no aparece un segundo registro: comun descartó el duplicado (eventos_procesados)
        await().during(Duration.ofSeconds(2)).atMost(ESPERA)
                .until(() -> contar("bitacora_kilometraje") == 1 && contar("eventos_procesados") == 1);
        assertThat(kilometrajeDelBus(bus)).isEqualByComparingTo("1100.00");
    }

    @Test
    void unEventoIlegibleTerminaEnLaDlq() {
        publicarCrudo("trips.eventos", "ViajeIniciado", "esto no es JSON");

        Message enDlq = rabbit.receive(DLQ, ESPERA.toMillis());

        assertThat(enDlq).isNotNull();
        assertThat(new String(enDlq.getBody(), StandardCharsets.UTF_8)).isEqualTo("esto no es JSON");
    }

    // ----- Identity -----

    @Test
    void perfilActualizadoRefrescaElNombreDelChoferYPublica() {
        UUID usuario = UUID.randomUUID();
        UUID chofer = insertarChofer(xela, usuario, "LIC-1", LocalDate.of(2028, 1, 1));

        publicarEvento("identity.eventos", "PerfilActualizado", usuario, """
                {"usuarioId":"%s","nombres":" Juan Carlos ","apellidos":"Perez","correo":"juan@fleet.gt","choferId":"%s"}
                """.formatted(usuario, chofer));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(jdbc.queryForObject(
                "SELECT nombre_completo FROM choferes WHERE id = ?", String.class, chofer))
                .isEqualTo("Juan Carlos Perez"));
        String choferActualizado = esperarEventos("ChoferActualizado").get("ChoferActualizado");
        assertThat(JsonPath.<String>read(choferActualizado, "$.payload.nombreCompleto")).isEqualTo("Juan Carlos Perez");
    }

    @Test
    void perfilActualizadoRefrescaNombreYCorreoDelPersonal() {
        UUID admin = insertarPersonal(xela, "ADMIN_SUCURSAL");
        UUID usuario = jdbc.queryForObject("SELECT usuario_id FROM personal_sucursal WHERE id = ?", UUID.class, admin);

        publicarEvento("identity.eventos", "PerfilActualizado", usuario, """
                {"usuarioId":"%s","nombres":"Ana Maria","apellidos":"Lopez","correo":"ana.maria@fleet.gt","choferId":null}
                """.formatted(usuario));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(jdbc.queryForObject(
                "SELECT correo FROM personal_sucursal WHERE id = ?", String.class, admin))
                .isEqualTo("ana.maria@fleet.gt"));
        assertThat(jdbc.queryForObject("SELECT nombre_completo FROM personal_sucursal WHERE id = ?", String.class,
                admin)).isEqualTo("Ana Maria Lopez");
    }
}
