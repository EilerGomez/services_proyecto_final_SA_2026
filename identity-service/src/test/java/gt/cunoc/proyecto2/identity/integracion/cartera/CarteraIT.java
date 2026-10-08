package gt.cunoc.proyecto2.identity.integracion.cartera;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** Cartera propia: saldo, recargas con fecha elegida e historial con filtros. */
class CarteraIT extends PruebaIntegracion {

    @Test
    void recargarSumaYApareceEnHistorial() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String token = tokenCliente(usuarioId, "ana@pruebas.gt");

        ResponseEntity<String> recarga = post("/identity/cartera/recargas", token,
                "{\"monto\":200.00,\"fechaMovimiento\":\"2026-10-20\"}");
        assertThat(recarga.getStatusCode().value() == 201).isTrue();
        assertThat(PruebaIntegracion.<String>json(recarga, "$.fechaMovimiento")).isEqualTo("2026-10-20");

        ResponseEntity<String> saldo = get("/identity/cartera", token);
        assertThat(PruebaIntegracion.<Double>json(saldo, "$.saldo")).isEqualTo(200.00);

        ResponseEntity<String> historial = get(
                "/identity/cartera/movimientos?desde=2026-10-01&hasta=2026-10-31", token);
        assertThat(PruebaIntegracion.<java.util.List<String>>json(historial, "$[*].tipo"))
                .contains("RECARGA");
    }

    @Test
    void recargaSinMontoSeRechaza() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String token = tokenCliente(usuarioId, "ana@pruebas.gt");

        esError(post("/identity/cartera/recargas", token,
                "{\"monto\":0,\"fechaMovimiento\":\"2026-10-20\"}"), 400, "VALIDACION");
    }
}
