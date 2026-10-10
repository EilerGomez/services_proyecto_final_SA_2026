package gt.cunoc.proyecto2.trips.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.errores.ErrorRespuesta;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.trips.errores.integraciones.ErrorIntegracion;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class TraductorErroresRemotosTest {

    /** Un 4xx como lo arma RestClient: con la función que convierte el cuerpo JSON. */
    private static HttpClientErrorException error(HttpStatus status, String codigo, String mensaje) {
        HttpClientErrorException e = new HttpClientErrorException(status);
        ErrorRespuesta cuerpo = new ErrorRespuesta(status.value(), codigo, mensaje,
                "/fleet/interno/buses/x/disponibilidad", Instant.now(), null, List.of());
        e.setBodyConvertFunction(tipo -> cuerpo);
        return e;
    }

    @Test
    void losErroresDelUsuarioPasanConElMismoCodigo() {
        NegocioException invalida = TraductorErroresRemotos.traducir(
                error(HttpStatus.BAD_REQUEST, "FECHA_INVALIDA", "La fecha no es válida"),
                ErrorIntegracion.FLEET_NO_DISPONIBLE);
        NegocioException conflicto = TraductorErroresRemotos.traducir(
                error(HttpStatus.CONFLICT, "MODIFICACION_CONCURRENTE", "Otro usuario lo cambió"),
                ErrorIntegracion.FLEET_NO_DISPONIBLE);
        NegocioException regla = TraductorErroresRemotos.traducir(
                error(HttpStatus.valueOf(422), "BUS_EN_TALLER", "El bus está en taller"),
                ErrorIntegracion.FLEET_NO_DISPONIBLE);

        assertThat(invalida.getStatus().value()).isEqualTo(400);
        assertThat(invalida.getCodigo()).isEqualTo("FECHA_INVALIDA");
        assertThat(conflicto.getStatus().value()).isEqualTo(409);
        assertThat(conflicto.getCodigo()).isEqualTo("MODIFICACION_CONCURRENTE");
        assertThat(conflicto.getMessage()).isEqualTo("Otro usuario lo cambió");
        assertThat(regla.getStatus().value()).isEqualTo(422);
        assertThat(regla.getCodigo()).isEqualTo("BUS_EN_TALLER");
        assertThat(regla.getMessage()).isEqualTo("El bus está en taller");
    }

    @Test
    void otros4xxSonUnProblemaDeConfiguracion() {
        NegocioException prohibido = TraductorErroresRemotos.traducir(
                error(HttpStatus.FORBIDDEN, "PROHIBIDO", "Sin permiso"), ErrorIntegracion.FLEET_NO_DISPONIBLE);
        NegocioException noExiste = TraductorErroresRemotos.traducir(
                error(HttpStatus.NOT_FOUND, "BUS_NO_EXISTE", "No existe"), ErrorIntegracion.FLEET_NO_DISPONIBLE);
        NegocioException sinToken = TraductorErroresRemotos.traducir(
                new HttpClientErrorException(HttpStatus.UNAUTHORIZED), ErrorIntegracion.FLEET_NO_DISPONIBLE);

        assertThat(prohibido.getStatus().value()).isEqualTo(503);
        assertThat(prohibido.getCodigo()).isEqualTo("FLEET_NO_DISPONIBLE");
        assertThat(noExiste.getStatus().value()).isEqualTo(503);
        assertThat(noExiste.getCodigo()).isEqualTo("FLEET_NO_DISPONIBLE");
        assertThat(sinToken.getCodigo()).isEqualTo("FLEET_NO_DISPONIBLE");
    }

    @Test
    void sinCodigoOSinCuerpoNoSeReconoce() {
        NegocioException sinCodigo = TraductorErroresRemotos.traducir(
                error(HttpStatus.BAD_REQUEST, null, "x"), ErrorIntegracion.FLEET_NO_DISPONIBLE);
        NegocioException sinCuerpo = TraductorErroresRemotos.traducir(
                new HttpClientErrorException(HttpStatus.BAD_REQUEST), ErrorIntegracion.FLEET_NO_DISPONIBLE);
        HttpClientErrorException nulo = new HttpClientErrorException(HttpStatus.CONFLICT);
        nulo.setBodyConvertFunction(tipo -> null);
        NegocioException cuerpoNulo = TraductorErroresRemotos.traducir(nulo, ErrorIntegracion.FLEET_NO_DISPONIBLE);

        assertThat(sinCodigo.getCodigo()).isEqualTo("FLEET_NO_DISPONIBLE");
        assertThat(sinCuerpo.getCodigo()).isEqualTo("FLEET_NO_DISPONIBLE");
        assertThat(cuerpoNulo.getCodigo()).isEqualTo("FLEET_NO_DISPONIBLE");
        assertThat(cuerpoNulo.getStatus().value()).isEqualTo(503);
    }
}
