package gt.cunoc.proyecto2.comun.errores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorRespuestaTest {

    @Test
    void sinListaDeErroresQuedaVacia() {
        ErrorRespuesta r = new ErrorRespuesta(400, "X", "m", "/r", Instant.now(), null, null);
        assertThat(r.errores()).isEmpty();
    }

    @Test
    void laListaDeErroresEsInmutable() {
        List<ErrorRespuesta.ErrorCampo> campos = new ArrayList<>();
        campos.add(new ErrorRespuesta.ErrorCampo("placa", "obligatoria"));
        ErrorRespuesta r = new ErrorRespuesta(400, "X", "m", "/r", Instant.now(), null, campos);

        assertThat(r.errores()).containsExactly(new ErrorRespuesta.ErrorCampo("placa", "obligatoria"));
        assertThatThrownBy(() -> r.errores().add(new ErrorRespuesta.ErrorCampo("a", "b")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
