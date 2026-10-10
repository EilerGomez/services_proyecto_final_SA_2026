package gt.cunoc.proyecto2.sales.utilidades;

import gt.cunoc.proyecto2.sales.errores.generales.ErrorGeneral;
import java.util.Objects;

public final class VerificadorVersion {

    private VerificadorVersion() {
    }

    public static void verificar(Integer versionActual, Integer versionEnviada) {
        if (!Objects.equals(versionActual, versionEnviada)) {
            throw ErrorGeneral.MODIFICACION_CONCURRENTE.excepcion();
        }
    }
}