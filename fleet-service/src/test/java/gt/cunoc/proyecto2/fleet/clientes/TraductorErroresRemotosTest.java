package gt.cunoc.proyecto2.fleet.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.errores.ErrorRespuesta;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.fleet.errores.integraciones.ErrorIntegracion;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class TraductorErroresRemotosTest {

    /** Un 4xx como lo arma RestClient: con la función que convierte el cuerpo JSON. */
    private static HttpClientErrorException error(HttpStatus status, String codigo, String mensaje) {
        HttpClientErrorException e = new HttpClientErrorException(status);
        ErrorRespuesta cuerpo = new ErrorRespuesta(status.value(), codigo, mensaje, "/identity/interno/cuentas-personal",
                Instant.now(), null, List.of());
        e.setBodyConvertFunction(tipo -> cuerpo);
        return e;
    }

    @Test
    void losErroresDelUsuarioPasanConElMismoCodigo() {
        NegocioException conflicto = TraductorErroresRemotos.traducir(
                error(HttpStatus.CONFLICT, "CORREO_EN_USO", "El correo ya está registrado"),
                ErrorIntegracion.IDENTITY_NO_DISPONIBLE);
        NegocioException regla = TraductorErroresRemotos.traducir(
                error(HttpStatus.valueOf(422), "DPI_EN_USO", "El DPI ya está registrado"),
                ErrorIntegracion.IDENTITY_NO_DISPONIBLE);

        assertThat(conflicto.getStatus().value()).isEqualTo(409);
        assertThat(conflicto.getCodigo()).isEqualTo("CORREO_EN_USO");
        assertThat(conflicto.getMessage()).isEqualTo("El correo ya está registrado");
        assertThat(regla.getStatus().value()).isEqualTo(422);
        assertThat(regla.getCodigo()).isEqualTo("DPI_EN_USO");
    }

    @Test
    void otros4xxSonUnProblemaDeConfiguracion() {
        NegocioException prohibido = TraductorErroresRemotos.traducir(
                error(HttpStatus.FORBIDDEN, "PROHIBIDO", "Sin permiso"), ErrorIntegracion.IDENTITY_NO_DISPONIBLE);

        assertThat(prohibido.getStatus().value()).isEqualTo(503);
        assertThat(prohibido.getCodigo()).isEqualTo("IDENTITY_NO_DISPONIBLE");
    }

    @Test
    void sinCodigoOSinCuerpoNoSeReconoce() {
        NegocioException sinCodigo = TraductorErroresRemotos.traducir(
                error(HttpStatus.BAD_REQUEST, null, "x"), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
        NegocioException sinCuerpo = TraductorErroresRemotos.traducir(
                new HttpClientErrorException(HttpStatus.BAD_REQUEST), ErrorIntegracion.TRIPS_NO_DISPONIBLE);

        assertThat(sinCodigo.getCodigo()).isEqualTo("TRIPS_NO_DISPONIBLE");
        assertThat(sinCuerpo.getCodigo()).isEqualTo("TRIPS_NO_DISPONIBLE");
    }
}
