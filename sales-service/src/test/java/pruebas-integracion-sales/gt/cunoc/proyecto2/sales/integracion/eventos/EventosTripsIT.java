package gt.cunoc.proyecto2.sales.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

/**
 * Eventos de Trips que consume Sales (cola sales.q): se publican en trips.eventos como lo haría el relay de
 * Trips y se espera el efecto en vista_viajes, asientos_viaje y ordenes_alquiler.
 */
class EventosTripsIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);
    private static final String FECHA_SALIDA = "2026-11-20T06:00:00-06:00";
    private static final BigDecimal PRECIO = new BigDecimal("75.00");

    /** Payload de ViajeProgramado / ViajeActualizado (campos de ViajeEvento y otros que Sales ignora). */
    private static String viaje(UUID viajeId, String tipo, UUID sucursalId, int capacidad, String precio,
                                String placa) {
        return """
                {"viajeId":"%s","tipo":"%s","rutaId":"%s","sucursalId":"%s","origenNombre":"Quetzaltenango",
                 "destinoNombre":"Guatemala","busId":"%s","busPlaca":"%s","capacidad":%d,"precioBoleto":%s,
                 "fechaSalida":"%s","choferId":"%s","estado":"PROGRAMADO"}
                """.formatted(viajeId, tipo, UUID.randomUUID(), sucursalId, UUID.randomUUID(), placa, capacidad,
                precio, FECHA_SALIDA, UUID.randomUUID());
    }

    private static String viajeIniciado(UUID viajeId, String tipo) {
        return "{\"viajeId\":\"%s\",\"tipo\":\"%s\",\"busId\":\"%s\"}".formatted(viajeId, tipo, UUID.randomUUID());
    }

    private static String cotizacion(UUID alquilerId, UUID sucursalId, UUID clienteUsuarioId, String nit,
                                     String nombre) {
        return """
                {"alquilerId":"%s","sucursalId":"%s","clienteUsuarioId":%s,"clienteNit":%s,"clienteNombre":%s,
                 "clienteCorreo":"colegio@correo.com","origen":"Quetzaltenango","destino":"Antigua Guatemala",
                 "precioConfirmado":2600.00,"kmEstimados":180.50}
                """.formatted(alquilerId, sucursalId, texto(clienteUsuarioId), texto(nit), texto(nombre));
    }

    private static String texto(Object valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private Map<String, Object> filaViaje(UUID viajeId) {
        return jdbc.queryForMap("SELECT * FROM vista_viajes WHERE viaje_id = ?", viajeId);
    }

    private int viajes(UUID viajeId) {
        return contarDonde("vista_viajes", "viaje_id", viajeId);
    }

    private int asientos(UUID viajeId) {
        return contarDonde("asientos_viaje", "viaje_id", viajeId);
    }

    private int asientosEn(UUID viajeId, String estado) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM asientos_viaje WHERE viaje_id = ? AND estado = ?",
                Integer.class, viajeId, estado);
        return total == null ? 0 : total;
    }

    private int contarDonde(String tabla, String columna, Object valor) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE " + columna + " = ?",
                Integer.class, valor);
        return total == null ? 0 : total;
    }

    private int contar(String tabla) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
        return total == null ? 0 : total;
    }

    // ----- ViajeProgramado -----

    @Test
    void viajeProgramadoRegularCreaLaVistaYSusAsientosDisponibles() {
        UUID viajeId = UUID.randomUUID();
        UUID xela = UUID.randomUUID();

        publicarEvento(TRIPS_EVENTOS, "ViajeProgramado", viajeId, 1L,
                viaje(viajeId, "REGULAR", xela, 30, "75.00", "P-123ABC"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(asientos(viajeId)).isEqualTo(30));
        Map<String, Object> fila = filaViaje(viajeId);
        assertThat(fila.get("sucursal_origen_id")).isEqualTo(xela);
        assertThat(fila.get("origen_nombre")).isEqualTo("Quetzaltenango");
        assertThat(fila.get("destino_nombre")).isEqualTo("Guatemala");
        assertThat(fila.get("bus_placa")).isEqualTo("P-123ABC");
        assertThat(fila.get("capacidad")).isEqualTo(30);
        assertThat((BigDecimal) fila.get("precio_boleto")).isEqualByComparingTo("75.00");
        assertThat(fila.get("estado")).isEqualTo("PROGRAMADO");
        assertThat(fila.get("version")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT fecha_salida FROM vista_viajes WHERE viaje_id = ?",
                OffsetDateTime.class, viajeId).toInstant()).isEqualTo(SALIDA.toInstant());

        assertThat(asientosEn(viajeId, "DISPONIBLE")).isEqualTo(30);
        assertThat(jdbc.queryForObject("SELECT min(numero_asiento) FROM asientos_viaje WHERE viaje_id = ?",
                Integer.class, viajeId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT max(numero_asiento) FROM asientos_viaje WHERE viaje_id = ?",
                Integer.class, viajeId)).isEqualTo(30);
    }

    @Test
    void losEventosDeViajesPrivadosSeProcesanSinTocarLaVista() {
        UUID viajeId = UUID.randomUUID();
        UUID xela = UUID.randomUUID();

        UUID programado = publicarEvento(TRIPS_EVENTOS, "ViajeProgramado", viajeId, 1L,
                viaje(viajeId, "PRIVADO", xela, 30, "2600.00", "P-123ABC"));
        UUID actualizado = publicarEvento(TRIPS_EVENTOS, "ViajeActualizado", viajeId, 2L,
                viaje(viajeId, "PRIVADO", xela, 45, "2600.00", "P-123ABC"));
        UUID iniciado = publicarEvento(TRIPS_EVENTOS, "ViajeIniciado", viajeId, 3L, viajeIniciado(viajeId, "PRIVADO"));

        await().atMost(ESPERA).until(() -> procesado(programado) && procesado(actualizado) && procesado(iniciado));
        assertThat(viajes(viajeId)).isZero();
        assertThat(asientos(viajeId)).isZero();
    }

    // ----- ViajeActualizado -----

    @Test
    void viajeActualizadoConMasCapacidadAgregaAsientosYCambiaElPrecio() {
        UUID xela = UUID.randomUUID();
        UUID viajeId = insertarViaje(xela, 40, "PROGRAMADO");
        // un asiento ya vendido no se toca
        jdbc.update("UPDATE asientos_viaje SET estado = 'VENDIDO', venta_id = ? "
                        + "WHERE viaje_id = ? AND numero_asiento = 3",
                UUID.randomUUID(), viajeId);

        publicarEvento(TRIPS_EVENTOS, "ViajeActualizado", viajeId, 1L,
                viaje(viajeId, "REGULAR", xela, 45, "80.50", "P-999ZZZ"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(asientos(viajeId)).isEqualTo(45));
        Map<String, Object> fila = filaViaje(viajeId);
        assertThat(fila.get("capacidad")).isEqualTo(45);
        assertThat((BigDecimal) fila.get("precio_boleto")).isEqualByComparingTo("80.50");
        assertThat(fila.get("bus_placa")).isEqualTo("P-999ZZZ");
        assertThat(fila.get("version")).isEqualTo(1);
        assertThat(estadosAsientos(viajeId, 3, 41, 45)).containsExactly("VENDIDO", "DISPONIBLE", "DISPONIBLE");
        assertThat(asientosEn(viajeId, "DISPONIBLE")).isEqualTo(44);
    }

    @Test
    void unEventoDeViajeMasViejoQueLaVistaSeIgnora() {
        UUID xela = UUID.randomUUID();
        UUID viajeId = insertarViaje(xela, 40, "PROGRAMADO");
        jdbc.update("UPDATE vista_viajes SET version = 5 WHERE viaje_id = ?", viajeId);

        UUID actualizado = publicarEvento(TRIPS_EVENTOS, "ViajeActualizado", viajeId, 3L,
                viaje(viajeId, "REGULAR", xela, 50, "99.00", "P-999ZZZ"));
        UUID iniciado = publicarEvento(TRIPS_EVENTOS, "ViajeIniciado", viajeId, 4L, viajeIniciado(viajeId, "REGULAR"));

        await().atMost(ESPERA).until(() -> procesado(actualizado) && procesado(iniciado));
        Map<String, Object> fila = filaViaje(viajeId);
        assertThat((BigDecimal) fila.get("precio_boleto")).isEqualByComparingTo("75.00");
        assertThat(fila.get("capacidad")).isEqualTo(40);
        assertThat(fila.get("bus_placa")).isEqualTo("P-123ABC");
        assertThat(fila.get("estado")).isEqualTo("PROGRAMADO");
        assertThat(fila.get("version")).isEqualTo(5);
        assertThat(asientos(viajeId)).isEqualTo(40);
    }

    // ----- ViajeIniciado -----

    @Test
    void viajeIniciadoDejaElViajeEnRuta() {
        UUID viajeId = insertarViaje(UUID.randomUUID(), 40, "PROGRAMADO");

        publicarEvento(TRIPS_EVENTOS, "ViajeIniciado", viajeId, 1L, viajeIniciado(viajeId, "REGULAR"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(filaViaje(viajeId).get("estado")).isEqualTo("EN_RUTA"));
        assertThat(filaViaje(viajeId).get("version")).isEqualTo(1);
        assertThat(asientos(viajeId)).isEqualTo(40);
    }

    @Test
    void viajeIniciadoDeUnViajeQueNoEstaEnLaVistaNoFalla() {
        UUID viajeId = UUID.randomUUID();

        UUID eventId = publicarEvento(TRIPS_EVENTOS, "ViajeIniciado", viajeId, 1L, viajeIniciado(viajeId, "REGULAR"));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        assertThat(viajes(viajeId)).isZero();
    }

    // ----- CotizacionAlquilerConfirmada -----

    @Test
    void cotizacionConfirmadaCreaLaOrdenPendienteYNoSeDuplica() {
        UUID alquilerId = UUID.randomUUID();
        UUID xela = UUID.randomUUID();
        UUID cliente = UUID.randomUUID();
        String payload = cotizacion(alquilerId, xela, cliente, null, "Colegio San José");

        publicarEvento(TRIPS_EVENTOS, "CotizacionAlquilerConfirmada", alquilerId, 2L, payload);

        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(contarDonde("ordenes_alquiler", "alquiler_id", alquilerId)).isEqualTo(1));
        Map<String, Object> orden = jdbc.queryForMap("SELECT * FROM ordenes_alquiler WHERE alquiler_id = ?",
                alquilerId);
        assertThat(orden.get("estado")).isEqualTo("PENDIENTE_PAGO");
        assertThat(orden.get("cliente_nit")).isEqualTo("CF");
        assertThat(orden.get("cliente_nombre")).isEqualTo("Colegio San José");
        assertThat(orden.get("cliente_correo")).isEqualTo("colegio@correo.com");
        assertThat(orden.get("cliente_usuario_id")).isEqualTo(cliente);
        assertThat(orden.get("sucursal_id")).isEqualTo(xela);
        assertThat(orden.get("origen")).isEqualTo("Quetzaltenango");
        assertThat(orden.get("destino")).isEqualTo("Antigua Guatemala");
        assertThat((BigDecimal) orden.get("monto")).isEqualByComparingTo("2600.00");
        assertThat(orden.get("metodo_pago")).isNull();
        assertThat(orden.get("fecha_pago")).isNull();
        assertThat(orden.get("version")).isEqualTo(0);

        // Trips repite la cotización (otro eventId): no se crea otra orden
        UUID repetido = publicarEvento(TRIPS_EVENTOS, "CotizacionAlquilerConfirmada", alquilerId, 2L, payload);
        await().atMost(ESPERA).until(() -> procesado(repetido));
        assertThat(contarDonde("ordenes_alquiler", "alquiler_id", alquilerId)).isEqualTo(1);
    }

    @Test
    void cotizacionDeMostradorSinCuentaNiNombreQuedaComoConsumidorFinal() {
        UUID alquilerId = UUID.randomUUID();

        publicarEvento(TRIPS_EVENTOS, "CotizacionAlquilerConfirmada", alquilerId, 1L,
                cotizacion(alquilerId, UUID.randomUUID(), null, " ", null));

        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(contarDonde("ordenes_alquiler", "alquiler_id", alquilerId)).isEqualTo(1));
        Map<String, Object> orden = jdbc.queryForMap("SELECT * FROM ordenes_alquiler WHERE alquiler_id = ?",
                alquilerId);
        assertThat(orden.get("cliente_usuario_id")).isNull();
        assertThat(orden.get("cliente_nit")).isEqualTo("CF");
        assertThat(orden.get("cliente_nombre")).isEqualTo("Consumidor final");
    }

    // ----- Idempotencia y DLQ -----

    @Test
    void unEventoRepetidoSeProcesaUnaSolaVez() {
        UUID viajeId = UUID.randomUUID();
        UUID xela = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        publicarEvento(TRIPS_EVENTOS, "ViajeProgramado", eventId, viajeId, 1L,
                viaje(viajeId, "REGULAR", xela, 30, "75.00", "P-123ABC"));
        // mismo eventId con otro contenido y versión mayor: si se procesara, cambiaría el precio
        publicarEvento(TRIPS_EVENTOS, "ViajeProgramado", eventId, viajeId, 2L,
                viaje(viajeId, "REGULAR", xela, 30, "90.00", "P-123ABC"));

        // Marcador: la cola se consume en orden, así que cuando el marcador termina el repetido ya pasó
        UUID marcador = publicarEvento(TRIPS_EVENTOS, "ViajeIniciado", UUID.randomUUID(), 1L,
                viajeIniciado(UUID.randomUUID(), "PRIVADO"));
        await().atMost(ESPERA).until(() -> procesado(marcador));

        assertThat(viajes(viajeId)).isEqualTo(1);
        assertThat((BigDecimal) filaViaje(viajeId).get("precio_boleto")).isEqualByComparingTo(PRECIO);
        assertThat(contar("eventos_procesados")).isEqualTo(2);           // el original y el marcador
        assertThat(filaViaje(viajeId).get("version")).isEqualTo(1);
        assertThat(asientos(viajeId)).isEqualTo(30);
    }

    @Test
    void unPayloadInvalidoTerminaEnLaDlqSinTocarLaVista() {
        UUID viajeId = UUID.randomUUID();
        String payload = viaje(viajeId, "REGULAR", UUID.randomUUID(), 30, "75.00", "P-123ABC")
                .replace("\"viajeId\":\"" + viajeId + "\"", "\"viajeId\":\"no-es-uuid\"");

        publicarEvento(TRIPS_EVENTOS, "ViajeProgramado", viajeId, 1L, payload);

        Message enDlq = rabbit.receive(DLQ, ESPERA.toMillis());
        assertThat(enDlq).isNotNull();
        assertThat(new String(enDlq.getBody(), StandardCharsets.UTF_8)).contains("no-es-uuid");
        assertThat(viajes(viajeId)).isZero();
        assertThat(contar("vista_viajes")).isZero();
        assertThat(contar("eventos_procesados")).isZero();             // se revirtió con la transacción
    }

    @Test
    void unEventoIlegibleTerminaEnLaDlq() {
        publicarCrudo(TRIPS_EVENTOS, "ViajeProgramado", "esto no es JSON");

        Message enDlq = rabbit.receive(DLQ, ESPERA.toMillis());

        assertThat(enDlq).isNotNull();
        assertThat(new String(enDlq.getBody(), StandardCharsets.UTF_8)).isEqualTo("esto no es JSON");
        assertThat(contar("eventos_procesados")).isZero();
    }
}
