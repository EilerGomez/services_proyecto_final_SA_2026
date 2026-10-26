package gt.cunoc.proyecto2.trips.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.trips.integracion.soporte.PruebaIntegracion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

/**
 * Eventos que consume Trips (cola trips.q): se publican en RabbitMQ como lo haría el relay de Fleet,
 * Identity o Sales y se espera el efecto en las vistas locales y en los alquileres.
 */
class EventosRecibidosIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);
    private static final String FLEET_EVENTOS = "fleet.eventos";

    private static String sucursal(UUID id, String nombre, boolean activa) {
        return """
                {"sucursalId":"%s","codigo":"XELA","nombre":"%s","latitud":14.834500,"longitud":-91.518000,
                 "activa":%s,"otroCampo":"Trips lo ignora"}
                """.formatted(id, nombre, activa);
    }

    private static final String FOTO_BUS = "https://almacen.local/buses/p-111aaa.jpg";

    private static String bus(UUID id, UUID sucursalId, String placa, int capacidad, String estado) {
        return """
                {"busId":"%s","sucursalId":"%s","placa":"%s","marca":"Volvo","capacidadPasajeros":%d,
                 "estado":"%s","fotoUrl":"%s"}
                """.formatted(id, sucursalId, placa, capacidad, estado, FOTO_BUS);
    }

    private static String chofer(UUID id, UUID sucursalId, String nombre, String salario, String estado) {
        return """
                {"choferId":"%s","sucursalId":"%s","nombreCompleto":"%s","salarioBaseViaje":%s,"estado":"%s"}
                """.formatted(id, sucursalId, nombre, salario, estado);
    }

    private Map<String, Object> fila(String tabla, UUID id) {
        return jdbc.queryForMap("SELECT * FROM " + tabla + " WHERE id = ?", id);
    }

    private int filas(String tabla, UUID id) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE id = ?", Integer.class, id);
        return total == null ? 0 : total;
    }

    // ----- Sucursales (Fleet) -----

    @Test
    void sucursalCreadaYActualizadaLlenanLaVista() {
        UUID id = UUID.randomUUID();

        publicarEvento(FLEET_EVENTOS, "SucursalCreada", id, 1, sucursal(id, "Quetzaltenango", true));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(filas("vista_sucursales", id)).isEqualTo(1));
        Map<String, Object> creada = fila("vista_sucursales", id);
        assertThat(creada.get("nombre")).isEqualTo("Quetzaltenango");
        assertThat((Boolean) creada.get("activa")).isTrue();
        assertThat((BigDecimal) creada.get("latitud")).isEqualByComparingTo("14.834500");
        assertThat(creada.get("version")).isEqualTo(1);

        publicarEvento(FLEET_EVENTOS, "SucursalActualizada", id, 2, sucursal(id, "Xela", false));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(fila("vista_sucursales", id).get("nombre"))
                .isEqualTo("Xela"));
        assertThat((Boolean) fila("vista_sucursales", id).get("activa")).isFalse();
        assertThat(fila("vista_sucursales", id).get("version")).isEqualTo(2);
    }

    @Test
    void unEventoViejoNoPisaLaVista() {
        UUID id = insertarSucursal("Quetzaltenango", true);
        jdbc.update("UPDATE vista_sucursales SET version = 5 WHERE id = ?", id);

        UUID eventId = publicarEvento(FLEET_EVENTOS, "SucursalActualizada", id, 3, sucursal(id, "Vieja", false));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        Map<String, Object> actual = fila("vista_sucursales", id);
        assertThat(actual.get("nombre")).isEqualTo("Quetzaltenango");
        assertThat(actual.get("version")).isEqualTo(5);
    }

    // ----- Buses (Fleet) -----

    @Test
    void busCreadoActualizadoYDesactivado() {
        UUID id = UUID.randomUUID();
        UUID xela = UUID.randomUUID();

        publicarEvento(FLEET_EVENTOS, "BusCreado", id, 0, bus(id, xela, "P-111AAA", 40, "ACTIVO"));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(filas("vista_buses", id)).isEqualTo(1));
        assertThat(fila("vista_buses", id).get("placa")).isEqualTo("P-111AAA");
        assertThat(fila("vista_buses", id).get("capacidad_pasajeros")).isEqualTo(40);
        // La foto llega en el evento: con ella el pasajero reconoce el bus al comprar el boleto
        assertThat(fila("vista_buses", id).get("foto_url")).isEqualTo(FOTO_BUS);

        publicarEvento(FLEET_EVENTOS, "BusActualizado", id, 1, bus(id, xela, "P-111AAA", 45, "EN_TALLER"));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(fila("vista_buses", id).get("estado"))
                .isEqualTo("EN_TALLER"));
        assertThat(fila("vista_buses", id).get("capacidad_pasajeros")).isEqualTo(45);

        publicarEvento(FLEET_EVENTOS, "BusDesactivado", id, 2, "{\"busId\":\"%s\",\"motivo\":\"Venta\"}".formatted(id));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(fila("vista_buses", id).get("estado"))
                .isEqualTo("INACTIVO"));
        assertThat(fila("vista_buses", id).get("version")).isEqualTo(2);
    }

    @Test
    void unBusDesactivadoViejoSeIgnora() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO vista_buses (id, sucursal_id, placa, capacidad_pasajeros, estado, version)
                VALUES (?, ?, 'P-111AAA', 40, 'ACTIVO', 4)""", id, UUID.randomUUID());

        UUID eventId = publicarEvento(FLEET_EVENTOS, "BusDesactivado", id, 2, "{\"busId\":\"%s\"}".formatted(id));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        assertThat(fila("vista_buses", id).get("estado")).isEqualTo("ACTIVO");
    }

    // ----- Choferes (Fleet) -----

    @Test
    void choferCreadoActualizadoYDesactivado() {
        UUID id = UUID.randomUUID();
        UUID xela = UUID.randomUUID();

        publicarEvento(FLEET_EVENTOS, "ChoferCreado", id, 0, chofer(id, xela, "Juan Perez", "350.00", "ACTIVO"));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(filas("vista_choferes", id)).isEqualTo(1));
        assertThat((BigDecimal) fila("vista_choferes", id).get("salario_base_viaje")).isEqualByComparingTo("350.00");

        publicarEvento(FLEET_EVENTOS, "ChoferActualizado", id, 1,
                chofer(id, xela, "Juan Carlos Perez", "375.50", "ACTIVO"));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(fila("vista_choferes", id).get("nombre_completo"))
                .isEqualTo("Juan Carlos Perez"));
        assertThat((BigDecimal) fila("vista_choferes", id).get("salario_base_viaje")).isEqualByComparingTo("375.50");

        publicarEvento(FLEET_EVENTOS, "ChoferDesactivado", id, 2, "{\"choferId\":\"%s\"}".formatted(id));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(fila("vista_choferes", id).get("estado"))
                .isEqualTo("INACTIVO"));
    }

    @Test
    void unEventoRepetidoSeProcesaUnaSolaVez() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        publicarEvento(FLEET_EVENTOS, "ChoferCreado", eventId, id, 1,
                chofer(id, UUID.randomUUID(), "Juan Perez", "350.00", "ACTIVO"));
        // mismo eventId con otro contenido: si se procesara, cambiaría el nombre
        publicarEvento(FLEET_EVENTOS, "ChoferCreado", eventId, id, 2,
                chofer(id, UUID.randomUUID(), "Otro Nombre", "350.00", "ACTIVO"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(filas("vista_choferes", id)).isEqualTo(1));
        await().during(Duration.ofSeconds(2)).atMost(ESPERA)
                .until(() -> "Juan Perez".equals(fila("vista_choferes", id).get("nombre_completo"))
                        && contar("eventos_procesados") == 1);
        assertThat(fila("vista_choferes", id).get("version")).isEqualTo(1);
    }

    // ----- Identity -----

    @Test
    void tarifaDepreciacionCambiadaSeGuardaUnaVez() {
        UUID tarifa = UUID.randomUUID();
        String payload = """
                {"tarifaId":"%s","montoPorKm":1.2500,"vigenteDesde":"2026-11-01"}""".formatted(tarifa);

        publicarEvento("identity.eventos", "TarifaDepreciacionCambiada", tarifa, 1, payload);
        await().atMost(ESPERA).untilAsserted(() -> assertThat(filas("vista_tarifas_depreciacion", tarifa))
                .isEqualTo(1));
        Map<String, Object> guardada = fila("vista_tarifas_depreciacion", tarifa);
        assertThat((BigDecimal) guardada.get("monto_por_km")).isEqualByComparingTo("1.25");
        assertThat(guardada.get("vigente_desde").toString()).isEqualTo("2026-11-01");

        // otro evento con la misma tarifa (otro eventId): ON CONFLICT DO NOTHING
        UUID repetido = publicarEvento("identity.eventos", "TarifaDepreciacionCambiada", tarifa, 1, payload);
        await().atMost(ESPERA).until(() -> procesado(repetido));
        assertThat(contar("vista_tarifas_depreciacion")).isEqualTo(1);
    }

    // ----- Clientes (Identity) -----

    @Test
    void clienteRegistradoYPerfilActualizadoMantienenLaCopia() {
        UUID usuarioId = UUID.randomUUID();

        publicarEvento("identity.eventos", "ClienteRegistrado", usuarioId, 0, """
                {"usuarioId":"%s","nit":"1234567-K","nombre":"Ana López","correo":"ana@correo.com"}
                """.formatted(usuarioId));

        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(contar("vista_clientes")).isEqualTo(1));
        Map<String, Object> cliente = jdbc.queryForMap("SELECT * FROM vista_clientes WHERE usuario_id = ?",
                usuarioId);
        assertThat(cliente.get("nit")).isEqualTo("1234567-K");
        assertThat(cliente.get("nombre")).isEqualTo("Ana López");

        // Cambia su perfil: el nombre viene partido y el NIT puede cambiar
        UUID eventId = publicarEvento("identity.eventos", "PerfilActualizado", usuarioId, 1, """
                {"usuarioId":"%s","nit":"7654321-0","nombres":"Ana María","apellidos":"López Coyoy",
                 "correo":"otro@correo.com"}
                """.formatted(usuarioId));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        Map<String, Object> actualizado = jdbc.queryForMap("SELECT * FROM vista_clientes WHERE usuario_id = ?",
                usuarioId);
        assertThat(actualizado.get("nit")).isEqualTo("7654321-0");
        assertThat(actualizado.get("nombre")).isEqualTo("Ana María López Coyoy");
        assertThat(actualizado.get("correo")).isEqualTo("otro@correo.com");
        assertThat(contar("vista_clientes")).isEqualTo(1);
    }

    @Test
    void alCrearSuCuentaAdoptaLosAlquileresQueLaVentanillaPidioConSuNit() {
        UUID sucursal = insertarSucursal("Quetzaltenango", true);
        UUID suyo = insertarAlquilerDeVentanilla(sucursal, "1234567-K");
        UUID deOtro = insertarAlquilerDeVentanilla(sucursal, "7654321-0");
        UUID consumidorFinal = insertarAlquilerDeVentanilla(sucursal, "CF");
        UUID usuarioId = UUID.randomUUID();

        publicarEvento("identity.eventos", "ClienteRegistrado", usuarioId, 0, """
                {"usuarioId":"%s","nit":"1234567-k","nombre":"Ana López","correo":"ana@correo.com"}
                """.formatted(usuarioId));

        // El suyo queda enlazado (el NIT viene del evento, sin importar mayúsculas)
        await().atMost(ESPERA).untilAsserted(() -> assertThat(clienteDelAlquiler(suyo)).isEqualTo(usuarioId));
        // Los de otro NIT y los de consumidor final no se tocan
        assertThat(clienteDelAlquiler(deOtro)).isNull();
        assertThat(clienteDelAlquiler(consumidorFinal)).isNull();
    }

    @Test
    void unClienteSinNitNoAdoptaLosAlquileresDeConsumidorFinal() {
        UUID sucursal = insertarSucursal("Quetzaltenango", true);
        UUID consumidorFinal = insertarAlquilerDeVentanilla(sucursal, "CF");
        UUID usuarioId = UUID.randomUUID();

        UUID eventId = publicarEvento("identity.eventos", "ClienteRegistrado", usuarioId, 0, """
                {"usuarioId":"%s","nit":null,"nombre":"Ana López","correo":"ana@correo.com"}
                """.formatted(usuarioId));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        assertThat(clienteDelAlquiler(consumidorFinal)).isNull();
    }

    /** Alquiler de mostrador: con NIT y sin cuenta, que es lo que se puede adoptar después. */
    private UUID insertarAlquilerDeVentanilla(UUID sucursalId, String nit) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO alquileres_privados (id, sucursal_id, canal, cliente_usuario_id, cliente_nit,
                                                 cliente_nombre, origen, destino, fecha_salida, cantidad_pasajeros,
                                                 km_estimados, precio_estimado, estado, fecha_solicitud,
                                                 solicitado_por)
                VALUES (?, ?, 'PRESENCIAL', NULL, ?, 'Maria Lopez', 'Xela', 'Antigua', ?, 30,
                        180.50, 2707.50, 'SOLICITADO', DATE '2026-10-01', ?)""",
                id, sucursalId, nit, SALIDA_ALQUILER, UUID.randomUUID());
        return id;
    }

    private UUID clienteDelAlquiler(UUID alquilerId) {
        return jdbc.queryForObject("SELECT cliente_usuario_id FROM alquileres_privados WHERE id = ?",
                UUID.class, alquilerId);
    }

    // ----- Sales -----

    @Test
    void alquilerPagadoPasaElAlquilerCotizadoAPagado() {
        UUID alquiler = insertarAlquiler(insertarSucursal("Quetzaltenango", true), UUID.randomUUID(), "COTIZADO");

        publicarEvento("sales.eventos", "AlquilerPagado", UUID.randomUUID(), 1,
                "{\"ordenId\":\"%s\",\"alquilerId\":\"%s\",\"monto\":2500.00}".formatted(UUID.randomUUID(), alquiler));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoDelAlquiler(alquiler)).isEqualTo("PAGADO"));
    }

    @Test
    void alquilerPagadoDeUnAlquilerNoCotizadoOInexistenteSeIgnora() {
        UUID alquiler = insertarAlquiler(insertarSucursal("Quetzaltenango", true), UUID.randomUUID(), "SOLICITADO");

        UUID primero = publicarEvento("sales.eventos", "AlquilerPagado", UUID.randomUUID(), 1,
                "{\"ordenId\":\"%s\",\"alquilerId\":\"%s\"}".formatted(UUID.randomUUID(), alquiler));
        UUID segundo = publicarEvento("sales.eventos", "AlquilerPagado", UUID.randomUUID(), 1,
                "{\"ordenId\":\"%s\",\"alquilerId\":\"%s\"}".formatted(UUID.randomUUID(), UUID.randomUUID()));

        await().atMost(ESPERA).until(() -> procesado(primero) && procesado(segundo));
        assertThat(estadoDelAlquiler(alquiler)).isEqualTo("SOLICITADO");
    }

    // ----- DLQ -----

    @Test
    void unEventoIlegibleTerminaEnLaDlq() {
        publicarCrudo(FLEET_EVENTOS, "BusCreado", "esto no es JSON");

        Message enDlq = rabbit.receive(DLQ, ESPERA.toMillis());

        assertThat(enDlq).isNotNull();
        assertThat(new String(enDlq.getBody(), StandardCharsets.UTF_8)).isEqualTo("esto no es JSON");
    }

    @Test
    void unPayloadIncompatibleTerminaEnLaDlqSinTocarLaVista() {
        UUID id = UUID.randomUUID();

        publicarEvento(FLEET_EVENTOS, "BusCreado", id, 1, """
                {"busId":"%s","sucursalId":"%s","placa":"P-111AAA","capacidadPasajeros":"muchos","estado":"ACTIVO"}
                """.formatted(id, UUID.randomUUID()));

        Message enDlq = rabbit.receive(DLQ, ESPERA.toMillis());
        assertThat(enDlq).isNotNull();
        assertThat(filas("vista_buses", id)).isZero();
        assertThat(contar("eventos_procesados")).isZero();             // se revirtió con la transacción
    }

    @Test
    void laTarifaRecibidaSeUsaAlRegistrarLaLlegada() {
        UUID tarifa = UUID.randomUUID();
        publicarEvento("identity.eventos", "TarifaDepreciacionCambiada", tarifa, 1,
                "{\"tarifaId\":\"%s\",\"montoPorKm\":2.0000,\"vigenteDesde\":\"%s\"}".formatted(tarifa,
                        LocalDate.of(2026, 1, 1)));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(filas("vista_tarifas_depreciacion", tarifa))
                .isEqualTo(1));

        UUID xela = insertarSucursal("Quetzaltenango", true);
        UUID ruta = insertarRuta(xela, insertarSucursal("Guatemala", true), true);
        UUID bus = UUID.randomUUID();
        UUID chofer = UUID.randomUUID();
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "EN_RUTA");
        insertarSalida(viaje, bus, chofer, SALIDA, "100.00");

        esStatus(post("/trips/viajes/" + viaje + "/llegada", tokenChofer(chofer),
                "{\"fechaHoraLlegada\":\"%s\",\"kmFinal\":150.00,\"costoCombustible\":10.00}".formatted(LLEGADA)), 200);

        assertThat(jdbc.queryForObject("SELECT costo_depreciacion FROM llegadas_viaje WHERE viaje_id = ?",
                BigDecimal.class, viaje)).isEqualByComparingTo("100.00");          // 50 km × 2.00
    }
}
