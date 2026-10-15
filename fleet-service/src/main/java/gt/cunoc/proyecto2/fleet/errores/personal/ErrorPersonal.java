package gt.cunoc.proyecto2.fleet.errores.personal;

import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import gt.cunoc.proyecto2.fleet.errores.TipoError;

public enum ErrorPersonal implements CodigoError {

    PERSONAL_NO_EXISTE(TipoError.NO_ENCONTRADO, "La persona no existe en el personal de sucursales"),
    PERSONAL_INACTIVO(TipoError.REGLA_NEGOCIO, "La persona ya está inactiva en esta sucursal"),
    SOLO_CAJEROS(TipoError.PROHIBIDO, "Un administrador de sucursal solo puede gestionar cajeros"),
    SUCURSAL_SIN_ADMIN(TipoError.REGLA_NEGOCIO, "La sucursal debe tener al menos un administrador activo"),
    TRASLADO_MISMA_SUCURSAL(TipoError.REGLA_NEGOCIO, "La persona ya trabaja en esa sucursal"),
    FECHA_DESASIGNACION_INVALIDA(TipoError.SOLICITUD_INVALIDA,
            "La fecha no puede ser anterior a la fecha de asignación (%s)"),
    PERSONAL_ACTIVO(TipoError.REGLA_NEGOCIO,
            "La persona sigue activa en su sucursal; para cambiarla de sucursal use el traslado"),
    PERSONAL_EN_OTRA_SUCURSAL(TipoError.CONFLICTO, "La persona ya trabaja en otra sucursal"),
    FECHA_REINTEGRO_INVALIDA(TipoError.SOLICITUD_INVALIDA,
            "La fecha de reintegro no puede ser anterior a la fecha de retiro (%s)"),
    PERSONAL_CUENTA_INACTIVA(TipoError.REGLA_NEGOCIO,
            "La cuenta de la persona está desactivada; actívela en Usuarios antes de reintegrarla"),
    PERSONAL_CUENTA_BLOQUEADA(TipoError.REGLA_NEGOCIO,
            "La cuenta de la persona está bloqueada %s; no se puede reintegrar mientras siga bloqueada");

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