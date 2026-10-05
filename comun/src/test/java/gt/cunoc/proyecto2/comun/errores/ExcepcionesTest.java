package gt.cunoc.proyecto2.comun.errores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

class ExcepcionesTest {

    static Stream<Arguments> excepciones() {
        return Stream.of(
                caso(SolicitudInvalidaException::new, SolicitudInvalidaException::new, HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA"),
                caso(NoAutenticadoException::new, NoAutenticadoException::new, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO"),
                caso(ProhibidoException::new, ProhibidoException::new, HttpStatus.FORBIDDEN, "PROHIBIDO"),
                caso(NoEncontradoException::new, NoEncontradoException::new, HttpStatus.NOT_FOUND, "NO_ENCONTRADO"),
                caso(ConflictoException::new, ConflictoException::new, HttpStatus.CONFLICT, "CONFLICTO"),
                caso(ReglaNegocioException::new, ReglaNegocioException::new, HttpStatus.UNPROCESSABLE_CONTENT, "REGLA_NEGOCIO"),
                caso(ServicioNoDisponibleException::new, ServicioNoDisponibleException::new,
                        HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE"));
    }

    private static Arguments caso(Function<String, NegocioException> soloMensaje,
                                  BiFunction<String, String, NegocioException> conCodigo,
                                  HttpStatus status, String codigoPorDefecto) {
        return Arguments.of(soloMensaje, conCodigo, status, codigoPorDefecto);
    }

    @ParameterizedTest
    @MethodSource("excepciones")
    void cadaExcepcionTieneSuStatusYCodigo(Function<String, NegocioException> soloMensaje,
                                           BiFunction<String, String, NegocioException> conCodigo,
                                           HttpStatus status, String codigoPorDefecto) {
        NegocioException porDefecto = soloMensaje.apply("mensaje");
        assertThat(porDefecto.getStatus()).isEqualTo(status);
        assertThat(porDefecto.getCodigo()).isEqualTo(codigoPorDefecto);
        assertThat(porDefecto.getMessage()).isEqualTo("mensaje");

        NegocioException propia = conCodigo.apply("CODIGO_PROPIO", "otro mensaje");
        assertThat(propia.getStatus()).isEqualTo(status);
        assertThat(propia.getCodigo()).isEqualTo("CODIGO_PROPIO");
        assertThat(propia.getMessage()).isEqualTo("otro mensaje");
    }
}
