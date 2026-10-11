package gt.cunoc.proyecto2.sales.integracion.asientos;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * S1: mapa de asientos (público). En el mapa RESERVADO vigente y VENDIDO se muestran como OCUPADO;
 * una reserva vencida cuenta como DISPONIBLE aunque la tarea aún no la haya liberado.
 */
class AsientosIT extends PruebaIntegracion {

    private UUID xela;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
    }

    private static String mapa(UUID viajeId) {
        return "/sales/viajes/" + viajeId + "/asientos";
    }

    private static OffsetDateTime fecha(ResponseEntity<String> respuesta, String ruta) {
        return OffsetDateTime.parse(PruebaIntegracion.<String>json(respuesta, ruta));
    }

    /** Asiento VENDIDO de una venta cualquiera (asientos_viaje no tiene FK a ventas). */
    private void vender(UUID viajeId, int numero) {
        jdbc.update("UPDATE asientos_viaje SET estado = 'VENDIDO', venta_id = ? WHERE viaje_id = ? AND numero_asiento = ?",
                UUID.randomUUID(), viajeId, numero);
    }

    /** Asiento RESERVADO: minutos > 0 = reserva vigente; minutos < 0 = reserva vencida. */
    private void reservar(UUID viajeId, int numero, int minutos) {
        jdbc.update("""
                UPDATE asientos_viaje SET estado = 'RESERVADO', venta_id = ?,
                       reservado_hasta = now() + make_interval(mins => ?)
                 WHERE viaje_id = ? AND numero_asiento = ?""",
                UUID.randomUUID(), minutos, viajeId, numero);
    }

    @Test
    void elMapaEsPublicoYMarcaOcupadosLosVendidosYLosReservados() {
        UUID viaje = insertarViaje(xela, 6, "PROGRAMADO");
        vender(viaje, 2);
        reservar(viaje, 3, 5);

        ResponseEntity<String> respuesta = get(mapa(viaje), null);

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.viajeId")).isEqualTo(viaje.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.origenNombre")).isEqualTo("Quetzaltenango");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.destinoNombre")).isEqualTo("Guatemala");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.busPlaca")).isEqualTo("P-123ABC");
        assertThat(fecha(respuesta, "$.fechaSalida")).isEqualTo(SALIDA);                       // con -06:00
        assertThat(decimal(respuesta, "$.precioBoleto")).isEqualByComparingTo("75.00");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.capacidad")).isEqualTo(6);
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.disponibles")).isEqualTo(4);
        assertThat(PruebaIntegracion.<List<Integer>>json(respuesta, "$.asientos[*].numero"))
                .containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$.asientos[*].estado"))
                .containsExactly("DISPONIBLE", "OCUPADO", "OCUPADO", "DISPONIBLE", "DISPONIBLE", "DISPONIBLE");
        // S1 lee la copia local: no consulta a Trips
        assertThat(TRIPS.peticiones()).isEmpty();
    }

    @Test
    void unaReservaVencidaSeMuestraDisponibleAunqueSigaReservadaEnLaBd() {
        UUID viaje = insertarViaje(xela, 4, "PROGRAMADO");
        reservar(viaje, 1, -1);
        reservar(viaje, 2, 5);

        ResponseEntity<String> respuesta = get(mapa(viaje), null);

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$.asientos[*].estado"))
                .containsExactly("DISPONIBLE", "OCUPADO", "DISPONIBLE", "DISPONIBLE");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.disponibles")).isEqualTo(3);
        assertThat(estadosAsientos(viaje, 1)).containsExactly("RESERVADO");     // la tarea aún no la libera
    }

    @Test
    void elMapaReflejaLasVentasEnMostradorYEnLinea() {
        UUID viaje = insertarViaje(xela);
        UUID cliente = UUID.randomUUID();
        insertarCliente(cliente, "1234567-K", "Ana Lopez", "ana@correo.com");
        tripsVendible(viaje);

        esStatus(post("/sales/ventas/mostrador", tokenCajero(xela), """
                {"viajeId":"%s","asientos":[1,2],"fechaPago":"%s"}""".formatted(viaje, FECHA_PAGO)), 201);
        esStatus(post("/sales/ventas/en-linea", tokenCliente(cliente), """
                {"viajeId":"%s","asientos":[3],"fechaPago":"%s"}""".formatted(viaje, FECHA_PAGO)), 202);

        ResponseEntity<String> respuesta = get(mapa(viaje), tokenCliente(cliente));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.disponibles")).isEqualTo(37);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$.asientos[0:4].estado"))
                .containsExactly("OCUPADO", "OCUPADO", "OCUPADO", "DISPONIBLE");
        assertThat(estadosAsientos(viaje, 1, 2, 3)).containsExactly("VENDIDO", "VENDIDO", "RESERVADO");
        assertThat(TRIPS.peticiones()).hasSize(2);                               // solo S4 y S2 llaman a R2
    }

    @Test
    void conCualquierTokenValidoTambienSeVe() {
        UUID viaje = insertarViaje(xela, 3, "PROGRAMADO");

        esStatus(get(mapa(viaje), tokenCliente(UUID.randomUUID())), 200);
        esStatus(get(mapa(viaje), tokenCajero(UUID.randomUUID())), 200);
        esStatus(get(mapa(viaje), token(UUID.randomUUID(), Roles.CHOFER, null)), 200);
        esStatus(get(mapa(viaje), tokenAdminSistema()), 200);
    }

    @Test
    void unViajeQueNoEstaALaVentaEs404() {
        UUID enRuta = insertarViaje(xela, 10, "EN_RUTA");
        UUID eliminado = insertarViaje(xela, 10, "PROGRAMADO");
        jdbc.update("DELETE FROM vista_viajes WHERE viaje_id = ?", eliminado);

        esError(get(mapa(enRuta), null), 404, "VIAJE_NO_DISPONIBLE");
        esError(get(mapa(eliminado), null), 404, "VIAJE_NO_DISPONIBLE");
        esError(get(mapa(UUID.randomUUID()), null), 404, "VIAJE_NO_DISPONIBLE");
    }

    @Test
    void unIdInvalidoEs400YUnTokenAlteradoEs401() {
        UUID viaje = insertarViaje(xela, 3, "PROGRAMADO");

        esError(get("/sales/viajes/no-es-uuid/asientos", null), 400, "PARAMETRO_INVALIDO");
        // el endpoint es público, pero un token que no es válido se rechaza en el filtro
        esError(get(mapa(viaje), "abc.def.ghi"), 401, "TOKEN_INVALIDO");
    }
}
