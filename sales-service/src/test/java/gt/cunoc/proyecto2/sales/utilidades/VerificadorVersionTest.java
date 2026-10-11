package gt.cunoc.proyecto2.sales.utilidades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.errores.generales.ErrorGeneral;
import org.junit.jupiter.api.Test;

class VerificadorVersionTest {

    @Test
    void mismaVersionNoLanzaError() {
        assertThatCode(() -> VerificadorVersion.verificar(3, 3)).doesNotThrowAnyException();
        assertThatCode(() -> VerificadorVersion.verificar(null, null)).doesNotThrowAnyException();
    }

    @Test
    void versionDistintaEsModificacionConcurrente() {
        VerificarError.lanza(() -> VerificadorVersion.verificar(4, 3), ErrorGeneral.MODIFICACION_CONCURRENTE);
        VerificarError.lanza(() -> VerificadorVersion.verificar(null, 0), ErrorGeneral.MODIFICACION_CONCURRENTE);
        VerificarError.lanza(() -> VerificadorVersion.verificar(0, null), ErrorGeneral.MODIFICACION_CONCURRENTE);
    }

    @Test
    void laModificacionConcurrenteEsUn409() {
        assertThatThrownBy(() -> VerificadorVersion.verificar(1, 0))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getStatus().value()).isEqualTo(409));
    }
}
