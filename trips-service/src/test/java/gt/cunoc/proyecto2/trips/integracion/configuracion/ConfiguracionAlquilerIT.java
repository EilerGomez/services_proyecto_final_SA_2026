package gt.cunoc.proyecto2.trips.integracion.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.trips.integracion.soporte.PruebaIntegracion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** T1 y T2: historial del precio por km del alquiler privado. */
class ConfiguracionAlquilerIT extends PruebaIntegracion {

    private static final String RUTA = "/trips/configuracion-alquiler";

    private static String nuevoPrecio(String precio, String vigenteDesde) {
        return "{\"precioPorKm\":%s,\"vigenteDesde\":\"%s\"}".formatted(precio, vigenteDesde);
    }

    // ----- T1 -----

    @Test
    void listarDevuelveElHistorialDelMasRecienteAlMasViejo() {
        insertarPrecioAlquiler("12.50", LocalDate.of(2026, 1, 1));
        insertarPrecioAlquiler("13.75", LocalDate.of(2026, 10, 1));

        ResponseEntity<String> deSistema = get(RUTA, tokenAdminSistema());
        ResponseEntity<String> deSucursal = get(RUTA, tokenAdminSucursal(UUID.randomUUID()));

        esStatus(deSistema, 200);
        assertThat(PruebaIntegracion.<List<String>>json(deSistema, "$[*].vigenteDesde"))
                .containsExactly("2026-10-01", "2026-01-01");
        assertThat(decimal(deSistema, "$[0].precioPorKm")).isEqualByComparingTo("13.75");
        esStatus(deSucursal, 200);
        assertThat(PruebaIntegracion.<List<Object>>json(deSucursal, "$")).hasSize(2);
    }

    @Test
    void elCajeroNoVeLaConfiguracionYSinTokenEs401() {
        esError(get(RUTA, tokenCajero(UUID.randomUUID())), 403, "ROL_INSUFICIENTE");
        esError(get(RUTA, null), 401, "NO_AUTENTICADO");
    }

    // ----- T2 -----

    @Test
    void registrarUnPrecioNuevoGuardaQuienLoCreo() {
        UUID admin = UUID.randomUUID();

        ResponseEntity<String> respuesta = post(RUTA, token(admin, Roles.ADMIN_SISTEMA, null, null),
                nuevoPrecio("14.25", "2026-12-01"));

        esStatus(respuesta, 201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.configuracionId")).isNotBlank();
        assertThat(decimal(respuesta, "$.precioPorKm")).isEqualByComparingTo("14.25");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.vigenteDesde")).isEqualTo("2026-12-01");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.creadoPor")).isEqualTo(admin.toString());
        assertThat(jdbc.queryForObject("SELECT creado_por FROM configuracion_alquiler", UUID.class)).isEqualTo(admin);
    }

    @Test
    void unaFechaQueYaTienePrecioEs409() {
        insertarPrecioAlquiler("12.50", LocalDate.of(2026, 12, 1));

        esError(post(RUTA, tokenAdminSistema(), nuevoPrecio("14.25", "2026-12-01")),
                409, "PRECIO_YA_EXISTE_PARA_FECHA");
        assertThat(contar("configuracion_alquiler")).isEqualTo(1);
    }

    @Test
    void soloElAdminDelSistemaRegistraPrecios() {
        esError(post(RUTA, tokenAdminSucursal(UUID.randomUUID()), nuevoPrecio("14.25", "2026-12-01")),
                403, "ROL_INSUFICIENTE");
        assertThat(contar("configuracion_alquiler")).isZero();
    }

    @Test
    void unPrecioInvalidoEs400() {
        esError(post(RUTA, tokenAdminSistema(), nuevoPrecio("0", "2026-12-01")), 400, "VALIDACION");
        esError(post(RUTA, tokenAdminSistema(), "{\"precioPorKm\":12.50}"), 400, "VALIDACION");
        esError(post(RUTA, tokenAdminSistema(), "{\"precioPorKm\":\"doce\"}"), 400, "CUERPO_INVALIDO");
    }
}
