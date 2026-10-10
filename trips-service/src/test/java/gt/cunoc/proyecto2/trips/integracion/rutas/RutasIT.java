package gt.cunoc.proyecto2.trips.integracion.rutas;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.trips.integracion.soporte.PruebaIntegracion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** T3–T7: rutas entre sucursales (vista_sucursales) y los eventos RutaCreada/Actualizada/Eliminada. */
class RutasIT extends PruebaIntegracion {

    private static final String RUTA = "/trips/rutas";

    private UUID xela;
    private UUID guate;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("Quetzaltenango", true);
        guate = insertarSucursal("Guatemala", true);
    }

    private static String nuevaRuta(UUID origen, UUID destino) {
        return """
                {"sucursalOrigenId":"%s","sucursalDestinoId":"%s","distanciaKm":205.50,"precioBoleto":75.00}
                """.formatted(origen, destino);
    }

    private static String cambio(String distancia, String precio, boolean activa, int version) {
        return "{\"distanciaKm\":%s,\"precioBoleto\":%s,\"activa\":%s,\"version\":%d}"
                .formatted(distancia, precio, activa, version);
    }

    // ----- T5 -----

    @Test
    void crearRutaConNombresDeSucursalesYPublicarRutaCreada() {
        ResponseEntity<String> respuesta = post(RUTA, tokenAdminSucursal(xela), nuevaRuta(xela, guate));

        esStatus(respuesta, 201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.origenNombre")).isEqualTo("Quetzaltenango");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.destinoNombre")).isEqualTo("Guatemala");
        assertThat(decimal(respuesta, "$.distanciaKm")).isEqualByComparingTo("205.50");
        assertThat(decimal(respuesta, "$.precioBoleto")).isEqualByComparingTo("75.00");
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.activa")).isTrue();
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isZero();
        String rutaId = json(respuesta, "$.rutaId");
        assertThat(contar("rutas")).isEqualTo(1);

        String evento = esperarEventos("RutaCreada").get("RutaCreada");
        assertThat(JsonPath.<String>read(evento, "$.aggregateId")).isEqualTo(rutaId);
        assertThat(versionDelEvento(evento)).isZero();
        assertThat(JsonPath.<String>read(evento, "$.payload.origenNombre")).isEqualTo("Quetzaltenango");
        assertThat(JsonPath.<String>read(evento, "$.payload.sucursalDestinoId")).isEqualTo(guate.toString());
        assertThat(JsonPath.<String>read(evento, "$.businessDate")).isEqualTo(hoy().toString());
    }

    @Test
    void unaRutaRepetidaEs409() {
        insertarRuta(xela, guate, true);

        esError(post(RUTA, tokenAdminSucursal(xela), nuevaRuta(xela, guate)), 409, "RUTA_DUPLICADA");
        assertThat(contar("rutas")).isEqualTo(1);
    }

    @Test
    void elRegresoEsOtraRuta() {
        insertarRuta(xela, guate, true);

        esStatus(post(RUTA, tokenAdminSucursal(guate), nuevaRuta(guate, xela)), 201);
        assertThat(contar("rutas")).isEqualTo(2);
    }

    @Test
    void unDestinoInactivoOInexistenteEs422() {
        UUID coban = insertarSucursal("Coban", false);

        esError(post(RUTA, tokenAdminSucursal(xela), nuevaRuta(xela, coban)), 422, "SUCURSAL_INACTIVA");
        esError(post(RUTA, tokenAdminSucursal(xela), nuevaRuta(xela, UUID.randomUUID())), 422, "SUCURSAL_NO_EXISTE");
        assertThat(contar("rutas")).isZero();
    }

    @Test
    void origenYDestinoIgualesEs422() {
        esError(post(RUTA, tokenAdminSucursal(xela), nuevaRuta(xela, xela)), 422, "MISMA_SUCURSAL");
    }

    @Test
    void unAdminNoCreaRutasDesdeOtraSucursal() {
        esError(post(RUTA, tokenAdminSucursal(guate), nuevaRuta(xela, guate)), 403, "SUCURSAL_AJENA");
        esError(post(RUTA, tokenCajero(xela), nuevaRuta(xela, guate)), 403, "ROL_INSUFICIENTE");
        esError(post(RUTA, null, nuevaRuta(xela, guate)), 401, "NO_AUTENTICADO");
    }

    @Test
    void unCuerpoIncompletoEs400() {
        esError(post(RUTA, tokenAdminSucursal(xela), "{\"sucursalOrigenId\":\"%s\"}".formatted(xela)),
                400, "VALIDACION");
    }

    // ----- T3 y T4 -----

    @Test
    void listarEsPublicoOrdenadoYConFiltros() {
        UUID huehue = insertarSucursal("Huehuetenango", true);
        insertarRuta(guate, xela, true);
        insertarRuta(xela, huehue, false);
        insertarRuta(xela, guate, true);

        ResponseEntity<String> todas = get(RUTA, null);
        ResponseEntity<String> deXela = get(RUTA + "?sucursalOrigenId=" + xela, null);
        ResponseEntity<String> inactivas = get(RUTA + "?activa=false", null);
        ResponseEntity<String> activasDeXela = get(RUTA + "?sucursalOrigenId=" + xela + "&activa=true", null);

        esStatus(todas, 200);
        assertThat(PruebaIntegracion.<List<String>>json(todas, "$[*].origenNombre"))
                .containsExactly("Guatemala", "Quetzaltenango", "Quetzaltenango");
        assertThat(PruebaIntegracion.<List<String>>json(todas, "$[*].destinoNombre"))
                .containsExactly("Quetzaltenango", "Guatemala", "Huehuetenango");
        assertThat(PruebaIntegracion.<List<Object>>json(deXela, "$")).hasSize(2);
        assertThat(PruebaIntegracion.<List<String>>json(inactivas, "$[*].destinoNombre"))
                .containsExactly("Huehuetenango");
        assertThat(PruebaIntegracion.<List<String>>json(activasDeXela, "$[*].destinoNombre"))
                .containsExactly("Guatemala");
    }

    @Test
    void detalleDeUnaRutaYRutaInexistenteEs404() {
        UUID ruta = insertarRuta(xela, guate, true);

        ResponseEntity<String> respuesta = get(RUTA + "/" + ruta, null);

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.rutaId")).isEqualTo(ruta.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.destinoNombre")).isEqualTo("Guatemala");
        esError(get(RUTA + "/" + UUID.randomUUID(), null), 404, "RUTA_NO_EXISTE");
        esError(get(RUTA + "/no-es-uuid", null), 400, "PARAMETRO_INVALIDO");
    }

    // ----- T6 -----

    @Test
    void actualizarRutaSubeLaVersionYPublicaRutaActualizada() {
        UUID ruta = insertarRuta(xela, guate, true);

        ResponseEntity<String> respuesta = put(RUTA + "/" + ruta, tokenAdminSucursal(xela),
                cambio("210.00", "80.00", false, 0));

        esStatus(respuesta, 200);
        assertThat(decimal(respuesta, "$.precioBoleto")).isEqualByComparingTo("80.00");
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.activa")).isFalse();
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT activa FROM rutas WHERE id = ?", Boolean.class, ruta)).isFalse();

        String evento = esperarEventos("RutaActualizada").get("RutaActualizada");
        assertThat(versionDelEvento(evento)).isEqualTo(1);
        assertThat(JsonPath.<Boolean>read(evento, "$.payload.activa")).isFalse();
        assertThat(decimal(evento, "$.payload.distanciaKm")).isEqualByComparingTo("210.00");
    }

    @Test
    void unaVersionViejaEs409() {
        UUID ruta = insertarRuta(xela, guate, true);
        jdbc.update("UPDATE rutas SET version = 3 WHERE id = ?", ruta);

        esError(put(RUTA + "/" + ruta, tokenAdminSucursal(xela), cambio("210.00", "80.00", true, 2)),
                409, "MODIFICACION_CONCURRENTE");
    }

    @Test
    void reactivarUnaRutaConDestinoInactivoEs422() {
        UUID ruta = insertarRuta(xela, guate, false);
        jdbc.update("UPDATE vista_sucursales SET activa = false WHERE id = ?", guate);

        esError(put(RUTA + "/" + ruta, tokenAdminSucursal(xela), cambio("200.00", "75.00", true, 0)),
                422, "SUCURSAL_INACTIVA");
        // sin reactivarla sí se puede editar
        esStatus(put(RUTA + "/" + ruta, tokenAdminSucursal(xela), cambio("200.00", "70.00", false, 0)), 200);
    }

    @Test
    void actualizarRutaDeOtraSucursalOInexistente() {
        UUID ruta = insertarRuta(xela, guate, true);

        esError(put(RUTA + "/" + ruta, tokenAdminSucursal(guate), cambio("200.00", "75.00", true, 0)),
                403, "SUCURSAL_AJENA");
        esError(put(RUTA + "/" + UUID.randomUUID(), tokenAdminSucursal(xela), cambio("200.00", "75.00", true, 0)),
                404, "RUTA_NO_EXISTE");
    }

    // ----- T7 -----

    @Test
    void eliminarRutaSinViajesPublicaRutaEliminadaConVersionSiguiente() {
        UUID ruta = insertarRuta(xela, guate, true);

        esStatus(delete(RUTA + "/" + ruta, tokenAdminSucursal(xela)), 204);

        assertThat(contar("rutas")).isZero();
        String evento = esperarEventos("RutaEliminada").get("RutaEliminada");
        assertThat(JsonPath.<String>read(evento, "$.payload.rutaId")).isEqualTo(ruta.toString());
        assertThat(versionDelEvento(evento)).isEqualTo(1);                 // version 0 + 1
    }

    @Test
    void unaRutaConViajesNoSeElimina() {
        UUID ruta = insertarRuta(xela, guate, true);
        insertarViajeRegular(ruta, xela, UUID.randomUUID(), UUID.randomUUID(), SALIDA, "COMPLETADO");

        esError(delete(RUTA + "/" + ruta, tokenAdminSucursal(xela)), 409, "RUTA_CON_VIAJES");
        assertThat(contar("rutas")).isEqualTo(1);
    }

    @Test
    void eliminarRutaDeOtraSucursalOInexistente() {
        UUID ruta = insertarRuta(xela, guate, true);

        esError(delete(RUTA + "/" + ruta, tokenAdminSucursal(guate)), 403, "SUCURSAL_AJENA");
        esError(delete(RUTA + "/" + UUID.randomUUID(), tokenAdminSucursal(xela)), 404, "RUTA_NO_EXISTE");
        assertThat(contar("rutas")).isEqualTo(1);
    }
}
