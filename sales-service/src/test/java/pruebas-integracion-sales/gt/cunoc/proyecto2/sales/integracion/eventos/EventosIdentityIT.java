package gt.cunoc.proyecto2.sales.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Eventos de Identity que alimentan vista_clientes (ClienteRegistrado y PerfilActualizado): se publican en
 * identity.eventos como lo haría el relay de Identity.
 */
class EventosIdentityIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);

    private static String clienteRegistrado(UUID usuarioId, String nit, String nombre, String correo) {
        return """
                {"usuarioId":"%s","nit":%s,"nombre":"%s","correo":"%s","rol":"CLIENTE"}
                """.formatted(usuarioId, nit == null ? "null" : "\"" + nit + "\"", nombre, correo);
    }

    private static String perfilActualizado(UUID usuarioId, String nit, String nombres, String apellidos,
                                            String correo) {
        return """
                {"usuarioId":"%s","nit":"%s","nombres":"%s","apellidos":%s,"correo":"%s","telefono":"55554444"}
                """.formatted(usuarioId, nit, nombres, apellidos == null ? "null" : "\"" + apellidos + "\"",
                correo);
    }

    private int clientes(UUID usuarioId) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM vista_clientes WHERE usuario_id = ?",
                Integer.class, usuarioId);
        return total == null ? 0 : total;
    }

    private Map<String, Object> cliente(UUID usuarioId) {
        return jdbc.queryForMap("SELECT * FROM vista_clientes WHERE usuario_id = ?", usuarioId);
    }

    // ----- ClienteRegistrado -----

    @Test
    void clienteRegistradoInsertaLaVistaDeClientes() {
        UUID usuario = UUID.randomUUID();

        publicarEvento(IDENTITY_EVENTOS, "ClienteRegistrado", usuario, null,
                clienteRegistrado(usuario, "1234567-8", "Ana Pérez", "ana@correo.com"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(clientes(usuario)).isEqualTo(1));
        Map<String, Object> fila = cliente(usuario);
        assertThat(fila.get("nit")).isEqualTo("1234567-8");
        assertThat(fila.get("nombre")).isEqualTo("Ana Pérez");
        assertThat(fila.get("correo")).isEqualTo("ana@correo.com");
    }

    @Test
    void clienteRegistradoSinNitQuedaComoConsumidorFinalYRepetidoNoDuplica() {
        UUID usuario = UUID.randomUUID();
        String payload = clienteRegistrado(usuario, null, "Luis Gómez", "luis@correo.com");

        publicarEvento(IDENTITY_EVENTOS, "ClienteRegistrado", usuario, null, payload);
        await().atMost(ESPERA).untilAsserted(() -> assertThat(clientes(usuario)).isEqualTo(1));
        assertThat(cliente(usuario).get("nit")).isEqualTo("CF");

        // Identity lo publica otra vez (otro eventId): upsert, sigue una sola fila
        UUID repetido = publicarEvento(IDENTITY_EVENTOS, "ClienteRegistrado", usuario, null, payload);
        await().atMost(ESPERA).until(() -> procesado(repetido));
        assertThat(clientes(usuario)).isEqualTo(1);
        assertThat(cliente(usuario).get("nombre")).isEqualTo("Luis Gómez");
    }

    @Test
    void conClienteRegistradoElClienteYaPuedeComprarEnLinea() {
        UUID usuario = UUID.randomUUID();
        UUID viaje = insertarViaje(UUID.randomUUID());
        tripsVendible(viaje);
        String compra = "{\"viajeId\":\"%s\",\"asientos\":[1],\"fechaPago\":\"%s\"}".formatted(viaje, FECHA_PAGO);

        // antes del evento todavía no hay perfil
        esError(post("/sales/ventas/en-linea", tokenCliente(usuario), compra), 422, "CLIENTE_SIN_PERFIL");

        publicarEvento(IDENTITY_EVENTOS, "ClienteRegistrado", usuario, null,
                clienteRegistrado(usuario, "1234567-8", "Ana Pérez", "ana@correo.com"));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(clientes(usuario)).isEqualTo(1));

        esStatus(post("/sales/ventas/en-linea", tokenCliente(usuario), compra), 202);
        assertThat(jdbc.queryForObject("SELECT cliente_nit FROM ventas WHERE cliente_usuario_id = ?",
                String.class, usuario)).isEqualTo("1234567-8");
    }

    // ----- PerfilActualizado -----

    @Test
    void perfilActualizadoCambiaNitNombreCompletoYCorreo() {
        UUID usuario = UUID.randomUUID();
        insertarCliente(usuario, "CF", "Ana", "ana@correo.com");

        publicarEvento(IDENTITY_EVENTOS, "PerfilActualizado", usuario, null,
                perfilActualizado(usuario, "7654321-0", "Ana María", "Pérez López", "ana.perez@correo.com"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(cliente(usuario).get("nombre"))
                .isEqualTo("Ana María Pérez López"));
        Map<String, Object> fila = cliente(usuario);
        assertThat(fila.get("nit")).isEqualTo("7654321-0");
        assertThat(fila.get("correo")).isEqualTo("ana.perez@correo.com");
        assertThat(clientes(usuario)).isEqualTo(1);
    }

    @Test
    void perfilActualizadoDeUnClienteQueNoEstabaLoAgregaSinApellidosVacios() {
        UUID usuario = UUID.randomUUID();

        publicarEvento(IDENTITY_EVENTOS, "PerfilActualizado", usuario, null,
                perfilActualizado(usuario, "1112223-4", " Carlos ", null, "carlos@correo.com"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(clientes(usuario)).isEqualTo(1));
        Map<String, Object> fila = cliente(usuario);
        assertThat(fila.get("nombre")).isEqualTo("Carlos");
        assertThat(fila.get("nit")).isEqualTo("1112223-4");
    }
}
