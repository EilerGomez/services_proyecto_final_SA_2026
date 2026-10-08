package gt.cunoc.proyecto2.fleet.integracion.choferes;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.fleet.integracion.soporte.PruebaIntegracion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** F15–F21 y R1b: choferes con Identity (R5) y Trips (R3) simulados. */
class ChoferesIT extends PruebaIntegracion {

    private static final String RUTA_R5 = "/identity/interno/cuentas-personal";
    private static final String RUTA_R3 = "/trips/interno/viajes/activos";
    private static final LocalDate VENCE = LocalDate.of(2028, 6, 30);

    private UUID xela;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("XELA", true);
    }

    private static String nuevoChofer(UUID sucursalId, UUID solicitudId, String vencimiento) {
        return """
                {"sucursalId":"%s","numeroLicencia":"lic-2024-77","tipoLicencia":"A",
                 "fechaVencimientoLicencia":"%s","salarioBaseViaje":350.00,"fechaRegistro":"2026-10-20",
                 "cuenta":{"solicitudId":"%s","correo":"juan@fleet.gt","nombres":"Juan","apellidos":"Perez",
                           "nit":"CF","dpi":"1234567890101","telefono":"55551234","direccion":"Zona 1",
                           "fotoUrl":"%s"}}
                """.formatted(sucursalId, vencimiento, solicitudId, FOTO_CHOFER);
    }

    @Test
    void crearChoferCreaLaCuentaYPublicaDosEventos() {
        UUID solicitudId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        IDENTITY.responder("POST", RUTA_R5, 201, cuentaCreada(usuarioId, "juan@fleet.gt", "Juan Perez"));

        ResponseEntity<String> respuesta = post("/fleet/choferes", tokenAdminSucursal(xela),
                nuevoChofer(xela, solicitudId, "2028-06-30"));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.choferId")).isEqualTo(solicitudId.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.usuarioId")).isEqualTo(usuarioId.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.numeroLicencia")).isEqualTo("LIC-2024-77");
        assertThat(IDENTITY.peticiones().get(0).cuerpo())
                .contains("\"rol\":\"CHOFER\"")
                .contains("\"choferId\":\"" + solicitudId + "\"");
        esperarEventos("ChoferCreado", "LicenciaChoferActualizada");
    }

    @Test
    void unaLicenciaVencidaNoSeRegistra() {
        esError(post("/fleet/choferes", tokenAdminSucursal(xela), nuevoChofer(xela, UUID.randomUUID(), "2026-10-19")),
                422, "LICENCIA_VENCIDA");
        assertThat(IDENTITY.peticiones()).isEmpty();
    }

    @Test
    void unaLicenciaRepetidaEs409() {
        insertarChofer(xela, UUID.randomUUID(), "LIC-2024-77", VENCE);

        esError(post("/fleet/choferes", tokenAdminSucursal(xela), nuevoChofer(xela, UUID.randomUUID(), "2028-06-30")),
                409, "LICENCIA_EN_USO");
    }

    @Test
    void elChoferConsultaSusPropiosDatos() {
        UUID chofer = insertarChofer(xela, UUID.randomUUID(), "LIC-1", VENCE);

        ResponseEntity<String> mio = get("/fleet/choferes/mio", tokenChofer(chofer));

        assertThat(mio.getStatusCode().value()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(mio, "$.choferId")).isEqualTo(chofer.toString());
        assertThat(get("/fleet/choferes/mio", tokenAdminSucursal(xela)).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void listarYObtener() {
        UUID chofer = insertarChofer(xela, UUID.randomUUID(), "LIC-1", VENCE);
        insertarChofer(insertarSucursal("GUATE", true), UUID.randomUUID(), "LIC-2", VENCE);

        assertThat(PruebaIntegracion.<List<String>>json(get("/fleet/choferes", tokenAdminSucursal(xela)),
                "$[*].numeroLicencia")).containsExactly("LIC-1");
        assertThat(PruebaIntegracion.<String>json(get("/fleet/choferes/" + chofer, tokenAdminSistema()),
                "$.nombreCompleto")).isEqualTo("Juan Perez");
    }

    @Test
    void actualizarSalarioYRenovarLicencia() {
        UUID chofer = insertarChofer(xela, UUID.randomUUID(), "LIC-1", VENCE);

        ResponseEntity<String> salario = put("/fleet/choferes/" + chofer, tokenAdminSucursal(xela),
                "{\"salarioBaseViaje\":400.00,\"version\":0}");
        ResponseEntity<String> licencia = put("/fleet/choferes/" + chofer + "/licencia", tokenAdminSucursal(xela),
                """
                {"numeroLicencia":"LIC-1","tipoLicencia":"B","fechaVencimientoLicencia":"2031-01-31",
                 "fechaRegistro":"2026-10-20","version":1}""");

        assertThat(salario.getStatusCode().value()).as(salario.getBody()).isEqualTo(200);
        assertThat(licencia.getStatusCode().value()).as(licencia.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(licencia, "$.tipoLicencia")).isEqualTo("B");
        assertThat(PruebaIntegracion.<Integer>json(licencia, "$.version")).isEqualTo(2);
        esperarEventos("ChoferActualizado", "LicenciaChoferActualizada");
    }

    @Test
    void noSeDesactivaConViajesYSiSinViajes() {
        UUID chofer = insertarChofer(xela, UUID.randomUUID(), "LIC-1", VENCE);
        String desactivar = "{\"fecha\":\"2026-10-20\",\"version\":0}";

        TRIPS.responder("GET", RUTA_R3, 200, "{\"cantidad\":1,\"viajes\":[]}");
        esError(patch("/fleet/choferes/" + chofer + "/desactivar", tokenAdminSucursal(xela), desactivar),
                422, "CHOFER_CON_VIAJES_ACTIVOS");
        assertThat(TRIPS.peticiones().get(0).consulta()).isEqualTo("choferId=" + chofer);

        TRIPS.responder("GET", RUTA_R3, 200, "{\"cantidad\":0,\"viajes\":[]}");
        ResponseEntity<String> respuesta = patch("/fleet/choferes/" + chofer + "/desactivar",
                tokenAdminSucursal(xela), desactivar);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("INACTIVO");
        esperarEventos("ChoferDesactivado");
    }

    @Test
    void consultaInternaParaProgramarUnViaje() {
        UUID chofer = insertarChofer(xela, UUID.randomUUID(), "LIC-1", VENCE);

        ResponseEntity<String> vigente = get("/fleet/interno/choferes/" + chofer + "?fecha=2028-06-30", tokenServicio());
        ResponseEntity<String> vencida = get("/fleet/interno/choferes/" + chofer + "?fecha=2028-07-01", tokenServicio());

        assertThat(vigente.getStatusCode().value()).isEqualTo(200);
        assertThat(PruebaIntegracion.<Boolean>json(vigente, "$.licenciaVigente")).isTrue();
        assertThat(PruebaIntegracion.<Boolean>json(vencida, "$.licenciaVigente")).isFalse();
        esError(get("/fleet/interno/choferes/" + UUID.randomUUID() + "?fecha=2028-06-30", tokenServicio()),
                404, "CHOFER_NO_EXISTE");
    }
}
