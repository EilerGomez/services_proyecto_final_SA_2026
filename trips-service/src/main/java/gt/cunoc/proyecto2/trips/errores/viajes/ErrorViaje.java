package gt.cunoc.proyecto2.trips.errores.viajes;

import gt.cunoc.proyecto2.trips.errores.CodigoError;
import gt.cunoc.proyecto2.trips.errores.TipoError;

public enum ErrorViaje implements CodigoError {

    VIAJE_NO_EXISTE(TipoError.NO_ENCONTRADO, "El viaje no existe"),
    VIAJE_PRIVADO(TipoError.PROHIBIDO,
            "Un viaje privado solo lo ven el personal de su sucursal y el chofer asignado"),
    SIN_CHOFER_ASOCIADO(TipoError.PROHIBIDO, "El usuario no tiene un chofer asociado"),
    RANGO_FECHAS_INVALIDO(TipoError.SOLICITUD_INVALIDA, "La fecha desde (%s) es posterior a la fecha hasta (%s)"),
    FILTRO_REQUERIDO(TipoError.SOLICITUD_INVALIDA, "Indique busId o choferId (solo uno)"),
    FECHAS_INVALIDAS(TipoError.SOLICITUD_INVALIDA, "La llegada estimada debe ser posterior a la salida"),
    RUTA_INACTIVA(TipoError.REGLA_NEGOCIO, "La ruta está inactiva; no se le programan viajes"),
    REGRESO_ANTES_DE_LA_IDA(TipoError.SOLICITUD_INVALIDA,
            "El viaje de regreso tiene que salir después que el de ida"),
    BUS_NO_DISPONIBLE(TipoError.REGLA_NEGOCIO, "El bus no se puede asignar: %s"),
    CHOFER_NO_DISPONIBLE(TipoError.REGLA_NEGOCIO, "El chofer no se puede asignar: %s"),
    LICENCIA_VENCIDA(TipoError.REGLA_NEGOCIO, "La licencia del chofer vence el %s, antes del viaje"),
    BUS_OCUPADO(TipoError.CONFLICTO, "El bus ya tiene un viaje programado o en ruta en ese horario"),
    CHOFER_OCUPADO(TipoError.CONFLICTO, "El chofer ya tiene un viaje programado o en ruta en ese horario"),
    VIAJE_YA_INICIO(TipoError.REGLA_NEGOCIO,
            "El viaje ya inició; solo se modifican o eliminan viajes programados"),
    CAPACIDAD_MENOR(TipoError.REGLA_NEGOCIO,
            "El bus nuevo tiene %d asientos y el viaje tiene %d: ya se pudieron vender esos asientos");

    private final TipoError tipo;
    private final String mensaje;

    ErrorViaje(TipoError tipo, String mensaje) {
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