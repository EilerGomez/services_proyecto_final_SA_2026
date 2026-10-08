package gt.cunoc.proyecto2.fleet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Afirma que una acción lanza el error de negocio esperado (por su código). */
public final class VerificarError {

    private VerificarError() {
    }

    public static void lanza(ThrowingCallable accion, CodigoError error) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(NegocioException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(error.codigo()));
    }
}
