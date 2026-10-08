package gt.cunoc.proyecto2.fleet.utilidades;

import static org.assertj.core.api.Assertions.assertThatCode;

import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import org.junit.jupiter.api.Test;

class VerificadorVersionTest {

    @Test
    void mismaVersionNoLanzaError() {
        assertThatCode(() -> VerificadorVersion.verificar(3, 3)).doesNotThrowAnyException();
    }

    @Test
    void versionDistintaEsModificacionConcurrente() {
        VerificarError.lanza(() -> VerificadorVersion.verificar(4, 3), ErrorGeneral.MODIFICACION_CONCURRENTE);
        VerificarError.lanza(() -> VerificadorVersion.verificar(null, 0), ErrorGeneral.MODIFICACION_CONCURRENTE);
    }
}
