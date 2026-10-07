/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.errores.personal;

/**
 *
 * @author eiler
 */

import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import gt.cunoc.proyecto2.fleet.errores.TipoError;

public enum ErrorPersonal implements CodigoError {

    PERSONAL_NO_EXISTE(TipoError.NO_ENCONTRADO, "La persona no existe en el personal de sucursales"),
    PERSONAL_INACTIVO(TipoError.REGLA_NEGOCIO, "La persona ya está inactiva en esta sucursal"),
    SOLO_CAJEROS(TipoError.PROHIBIDO, "Un administrador de sucursal solo puede gestionar cajeros"),
    SUCURSAL_SIN_ADMIN(TipoError.REGLA_NEGOCIO, "La sucursal debe tener al menos un administrador activo"),
    TRASLADO_MISMA_SUCURSAL(TipoError.REGLA_NEGOCIO, "La persona ya trabaja en esa sucursal"),
    SOLICITUD_YA_USADA(TipoError.CONFLICTO, "El solicitudId ya se usó para otra sucursal; genere uno nuevo"),
    FECHA_DESASIGNACION_INVALIDA(TipoError.SOLICITUD_INVALIDA,
            "La fecha no puede ser anterior a la fecha de asignación (%s)");

    private final TipoError tipo;
    private final String mensaje;

    ErrorPersonal(TipoError tipo, String mensaje) {
        this.tipo = tipo;
        this.mensaje = mensaje;
    }

    @Override
    public TipoError tipo() {
        return tipo;
    }

    @Override
    public String mensaje() {
        return mensaje;
    }
}
