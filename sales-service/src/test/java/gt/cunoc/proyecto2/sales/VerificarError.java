package gt.cunoc.proyecto2.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.sales.errores.CodigoError;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Afirma que una acción lanza el error de negocio esperado (por su código). */
public final class VerificarError {

    private VerificarError() {
    }

    public static void lanza(ThrowingCallable accion, CodigoError error) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(NegocioException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(error.codigo()));
    }

    /** Para errores que no son de un enum de Sales (p. ej. SUCURSAL_AJENA de comun). */
    public static void lanza(ThrowingCallable accion, String codigo) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(NegocioException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }
}
