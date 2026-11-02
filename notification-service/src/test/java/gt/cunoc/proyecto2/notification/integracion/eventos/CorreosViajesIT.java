package gt.cunoc.proyecto2.notification.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.notification.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Correos de los viajes: el aviso al piloto cuando le asignan un viaje (ViajeProgramado, de Trips)
 * y el aviso de reasignación por sus dos caminos, porque el destinatario vive en servicios
 * distintos: el cliente del alquiler privado lo sabe Trips (ViajeReasignado) y el pasajero con
 * boleto lo sabe Sales (BoletoReasignado).
 */
class CorreosViajesIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);
    private static final String CLIENTE = "cliente.alquiler@correo.com";
    private static final String PASAJERO = "pasajero@correo.com";

    /** ViajeProgramado de Trips; con choferUsuarioId en null no hay a quién escribirle. */
    private static String viajeProgramado(String tipo, String sentido, UUID choferUsuarioId) {
        return """
                {"viajeId":"%s","tipo":"%s","sucursalId":"%s","origenNombre":"Quetzaltenango",
                 "destinoNombre":"Guatemala","busPlaca":"P-123ABC","choferId":"%s",
                 "choferNombre":"Juan Perez","choferUsuarioId":%s,
                 "fechaSalida":"2026-11-05T08:00:00-06:00",
                 "fechaLlegadaEstimada":"2026-11-05T12:00:00-06:00","sentido":"%s"}
                """.formatted(UUID.randomUUID(), tipo, UUID.randomUUID(), UUID.randomUUID(),
                choferUsuarioId == null ? "null" : "\"" + choferUsuarioId + "\"", sentido);
    }

    /** ViajeReasignado de Trips: sin correo del cliente (viaje regular) no se escribe nada. */
    private static String viajeReasignado(boolean cambioBus, boolean cambioChofer, String clienteCorreo) {
        return """
                {"viajeId":"%s","tipo":"PRIVADO","origenNombre":"Quetzaltenango",
                 "destinoNombre":"Guatemala","fechaSalida":"2026-11-05T08:00:00-06:00",
                 "cambioBus":%s,"busPlacaAnterior":"P-111AAA","busPlaca":"P-222BBB",
                 "cambioChofer":%s,"choferAnterior":"Juan Perez","choferNombre":"Pedro Lopez",
                 "clienteNombre":"Ana Lopez","clienteCorreo":%s}
                """.formatted(UUID.randomUUID(), cambioBus, cambioChofer,
                clienteCorreo == null ? "null" : "\"" + clienteCorreo + "\"");
    }

    /** BoletoReasignado de Sales, uno por pasajero con venta confirmada y correo. */
    private static String boletoReasignado(String asientosJson, String clienteNombre,
                                           boolean cambioBus, boolean cambioChofer) {
        return """
                {"ventaId":"%s","codigo":"VTA-0001","viajeId":"%s","clienteNombre":%s,
                 "clienteCorreo":"%s","asientos":%s,"origenNombre":"Quetzaltenango",
                 "destinoNombre":"Guatemala","fechaSalida":"2026-11-05T08:00:00-06:00",
                 "cambioBus":%s,"busPlacaAnterior":"P-111AAA","busPlaca":"P-222BBB",
                 "cambioChofer":%s,"choferAnterior":"Juan Perez","choferNombre":"Pedro Lopez"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(),
                clienteNombre == null ? "null" : "\"" + clienteNombre + "\"",
                PASAJERO, asientosJson, cambioBus, cambioChofer);
    }

    // ----- ViajeProgramado: aviso al piloto -----

    @Test
    void alProgramarUnViajeRegularDeIdaSeLeAvisaAlPiloto() {
        UUID eventId = publicarEvento(TRIPS_EVENTOS, "ViajeProgramado", "2026-11-01",
                viajeProgramado("REGULAR", "IDA", UUID.randomUUID()));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(asuntosPara(CHOFER_CORREO))
                .containsExactly("Viaje asignado: Quetzaltenango → Guatemala"));
        assertThat(textoPara(CHOFER_CORREO)).contains(
                "Hola Chofer Prueba",
                "Viaje: Quetzaltenango → Guatemala",
                "Sale: 05/11/2026 08:00",
                "Bus: P-123ABC");
        assertThat(notificaciones(eventId)).hasSize(1);
        assertThat(notificaciones(eventId).get(0))
                .containsEntry("plantilla", "VIAJE_ASIGNADO_PILOTO")
                .containsEntry("destinatario", CHOFER_CORREO)
                .containsEntry("estado", "ENVIADA");
    }

    @Test
    void elViajeDeRegresoSeLeAvisaComoTalParaNoConfundirlo() {
        UUID eventId = publicarEvento(TRIPS_EVENTOS, "ViajeProgramado", "2026-11-01",
                viajeProgramado("REGULAR", "REGRESO", UUID.randomUUID()));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara(CHOFER_CORREO)).hasSize(1));
        assertThat(textoPara(CHOFER_CORREO)).contains("Viaje de regreso: Quetzaltenango → Guatemala");
    }

    @Test
    void elViajeDeUnAlquilerPrivadoSeLeAvisaComoAlquiler() {
        UUID eventId = publicarEvento(TRIPS_EVENTOS, "ViajeProgramado", "2026-11-01",
                viajeProgramado("PRIVADO", "IDA", UUID.randomUUID()));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara(CHOFER_CORREO)).hasSize(1));
        assertThat(textoPara(CHOFER_CORREO)).contains("Alquiler privado: Quetzaltenango → Guatemala");
    }

    @Test
    void unViajeSinLaCuentaDelPilotoNoLePreguntaNadaAIdentityNiEscribe() {
        UUID eventId = publicarEvento(TRIPS_EVENTOS, "ViajeProgramado", "2026-11-01",
                viajeProgramado("REGULAR", "IDA", null));

        await().atMost(ESPERA).until(() -> procesado(eventId));

        assertThat(totalNotificaciones()).isZero();
        assertThat(totalCorreos()).isZero();
    }

    // ----- ViajeReasignado: cliente del alquiler privado -----

    @Test
    void alClienteDelAlquilerSeLeAvisaSoloElCambioDeBus() {
        UUID eventId = publicarEvento(TRIPS_EVENTOS, "ViajeReasignado", "2026-11-01",
                viajeReasignado(true, false, CLIENTE));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(asuntosPara(CLIENTE))
                .containsExactly("Cambio de bus: Quetzaltenango → Guatemala"));
        String texto = textoPara(CLIENTE);
        assertThat(texto).contains(
                "Hola Ana Lopez",
                "Hubo un cambio en el bus que contrataste",
                "El bus asignado ahora es el P-222BBB (antes era el P-111AAA).",
                "el precio de tu alquiler no cambian");
        // solo cambió el bus: del piloto no se dice nada
        assertThat(texto).doesNotContain("El piloto a cargo");
        assertThat(notificaciones(eventId).get(0)).containsEntry("plantilla", "VIAJE_REASIGNADO");
    }

    @Test
    void alClienteDelAlquilerSeLeAvisaSoloElCambioDePiloto() {
        UUID eventId = publicarEvento(TRIPS_EVENTOS, "ViajeReasignado", "2026-11-01",
                viajeReasignado(false, true, CLIENTE));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(asuntosPara(CLIENTE))
                .containsExactly("Cambio de piloto: Quetzaltenango → Guatemala"));
        String texto = textoPara(CLIENTE);
        assertThat(texto).contains("El piloto a cargo ahora es Pedro Lopez (antes era Juan Perez).");
        assertThat(texto).doesNotContain("El bus asignado ahora es");
    }

    @Test
    void elViajeRegularReasignadoLlegaSinCorreoYTripsNoEscribeANadie() {
        // En un REGULAR el cliente no existe: a cada pasajero le escribe el BoletoReasignado de Sales
        UUID eventId = publicarEvento(TRIPS_EVENTOS, "ViajeReasignado", "2026-11-01",
                viajeReasignado(true, true, null));

        await().atMost(ESPERA).until(() -> procesado(eventId));

        assertThat(totalNotificaciones()).isZero();
        assertThat(totalCorreos()).isZero();
    }

    // ----- BoletoReasignado: pasajero del viaje regular -----

    @Test
    void alPasajeroSeLeAvisanLosDosCambiosConSusAsientos() {
        UUID eventId = publicarEvento(SALES_EVENTOS, "BoletoReasignado", "2026-11-01",
                boletoReasignado("[3,4]", "Ana Lopez", true, true));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(asuntosPara(PASAJERO))
                .containsExactly("Cambio de bus y de piloto: Quetzaltenango → Guatemala"));
        String texto = textoPara(PASAJERO);
        assertThat(texto).contains(
                "Hola Ana Lopez",
                "Hubo un cambio en tu viaje Quetzaltenango → Guatemala",
                "El bus asignado ahora es el P-222BBB (antes era el P-111AAA).",
                "El piloto a cargo ahora es Pedro Lopez (antes era Juan Perez).",
                "Tus asientos siguen siendo los mismos: 3, 4.");
        assertThat((String) notificaciones(eventId).get(0).get("datos"))
                .contains("VTA-0001", "asientos");
    }

    @Test
    void unBoletoSinAsientosNiNombreSeAvisaConGuiones() {
        UUID eventId = publicarEvento(SALES_EVENTOS, "BoletoReasignado", "2026-11-01",
                boletoReasignado("[]", null, true, false));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara(PASAJERO)).hasSize(1));
        assertThat(textoPara(PASAJERO)).contains("Hola -", "Tus asientos siguen siendo los mismos: -.");
    }

    /**
     * El mismo aviso con lo mínimo que puede traer el evento: sin ids, con la lista de asientos en
     * null, el nombre en blanco y un destino tan largo que el asunto hay que recortarlo.
     */
    @Test
    void unBoletoAlQueLeFaltanDatosSeAvisaIgualYConElAsuntoRecortado() {
        String destinoLargo = "G".repeat(250);
        String payload = """
                {"ventaId":null,"codigo":"VTA-0002","viajeId":null,"clienteNombre":"  ",
                 "clienteCorreo":"%s","asientos":null,"origenNombre":"Quetzaltenango",
                 "destinoNombre":"%s","fechaSalida":"2026-11-05T08:00:00-06:00",
                 "cambioBus":false,"busPlacaAnterior":"P-111AAA","busPlaca":"P-222BBB",
                 "cambioChofer":true,"choferAnterior":"Juan Perez","choferNombre":"Pedro Lopez"}
                """.formatted(PASAJERO, destinoLargo);

        UUID eventId = publicarEvento(SALES_EVENTOS, "BoletoReasignado", "2026-11-01", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara(PASAJERO)).hasSize(1));
        // el asunto se corta a 200 caracteres: la columna de la base no aguanta más
        String asunto = asuntosPara(PASAJERO).get(0);
        assertThat(asunto).startsWith("Cambio de piloto: Quetzaltenango → G").doesNotContain(destinoLargo);
        assertThat(textoPara(PASAJERO)).contains("Hola -", "Tus asientos siguen siendo los mismos: -.");
        assertThat((String) notificaciones(eventId).get(0).get("datos"))
                .contains("\"ventaId\": null", "\"viajeId\": null");
    }
}
