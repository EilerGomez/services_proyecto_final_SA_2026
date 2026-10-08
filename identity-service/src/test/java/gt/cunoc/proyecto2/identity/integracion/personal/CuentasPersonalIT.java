package gt.cunoc.proyecto2.identity.integracion.personal;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** R5: alta idempotente de personal y choferes desde Fleet. */
class CuentasPersonalIT extends PruebaIntegracion {

    private static final UUID SUCURSAL = UUID.randomUUID();

    @Test
    void crearYReintentarDevuelveLaMismaCuenta() {
        UUID solicitud = UUID.randomUUID();
        String cuerpo = """
                {"solicitudId":"%s","rol":"CAJERO","sucursalId":"%s","choferId":null,
                 "correo":"cajero@pruebas.gt","nombres":"Caja","apellidos":"Era",
                 "nit":"4000000-4","dpi":"4000000000404","telefono":"55550004","direccion":"Xela",
                 "fotoUrl":null}
                """.formatted(solicitud, SUCURSAL);

        ResponseEntity<String> primera =
                post("/identity/interno/cuentas-personal", tokenServicio(), cuerpo);
        assertThat(primera.getStatusCode().value() == 201).isTrue();
        assertThat(PruebaIntegracion.<Boolean>json(primera, "$.creada")).isTrue();

        ResponseEntity<String> reintento =
                post("/identity/interno/cuentas-personal", tokenServicio(), cuerpo);
        assertThat(reintento.getStatusCode().value() == 200).isTrue();
        assertThat(PruebaIntegracion.<Boolean>json(reintento, "$.creada")).isFalse();
        assertThat(PruebaIntegracion.<String>json(reintento, "$.usuarioId"))
                .isEqualTo(json(primera, "$.usuarioId"));

        Map<String, String> eventos = esperarEventos("CuentaCreada", "ClienteRegistrado");
        assertThat(eventos.get("CuentaCreada")).contains("/restablecer?token=");
    }

    @Test
    void choferSinFotoSeRechaza() {
        String cuerpo = """
                {"solicitudId":"%s","rol":"CHOFER","sucursalId":"%s","choferId":"%s",
                 "correo":"chofer@pruebas.gt","nombres":"Pedro","apellidos":"Ramírez",
                 "nit":"5000000-5","dpi":"5000000000505","telefono":"55550005","direccion":"Xela",
                 "fotoUrl":null}
                """.formatted(UUID.randomUUID(), SUCURSAL, UUID.randomUUID());

        esError(post("/identity/interno/cuentas-personal", tokenServicio(), cuerpo),
                422, "FOTO_OBLIGATORIA");
    }

    @Test
    void sinRolServicioDa403() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String token = tokenCliente(usuarioId, "ana@pruebas.gt");

        esError(post("/identity/interno/cuentas-personal", token,
                "{\"solicitudId\":\"%s\"}".formatted(UUID.randomUUID())), 403, "ROL_INSUFICIENTE");
    }
}
