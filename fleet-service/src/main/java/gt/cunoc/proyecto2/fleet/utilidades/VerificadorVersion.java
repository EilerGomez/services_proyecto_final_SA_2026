/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.utilidades;

/**
 *
 * @author eiler
 */
import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
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
