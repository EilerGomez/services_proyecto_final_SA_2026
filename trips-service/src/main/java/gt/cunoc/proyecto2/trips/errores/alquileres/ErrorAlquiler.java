package gt.cunoc.proyecto2.trips.errores.alquileres;

import gt.cunoc.proyecto2.trips.errores.CodigoError;
import gt.cunoc.proyecto2.trips.errores.TipoError;

/** Errores de alquileres privados (T16–T22). */
public enum ErrorAlquiler implements CodigoError {

    ALQUILER_NO_EXISTE(TipoError.NO_ENCONTRADO, "El alquiler no existe"),
    ALQUILER_AJENO(TipoError.PROHIBIDO,
            "Un alquiler solo lo ven su cliente y el personal de la sucursal que lo atiende"),
    RETORNO_ANTES_DE_SALIDA(TipoError.SOLICITUD_INVALIDA, "La fecha de retorno no puede ser anterior a la de salida"),
    FECHA_SOLICITUD_PASADA(TipoError.SOLICITUD_INVALIDA,
            "La fecha de solicitud (%s) no puede ser anterior a la de hoy (%s)"),    
    ESTADO_INVALIDO(TipoError.REGLA_NEGOCIO, "El alquiler está %s; esta acción solo aplica a alquileres %s"),
    ALQUILER_NO_PAGADO(TipoError.REGLA_NEGOCIO, "El alquiler está %s; solo se asigna bus y chofer a un alquiler PAGADO"),
    CAPACIDAD_INSUFICIENTE(TipoError.REGLA_NEGOCIO, "El bus tiene %d asientos y el alquiler es para %d pasajeros"),
    COORDENADAS_INCOMPLETAS(TipoError.SOLICITUD_INVALIDA,
            "Marca el recorrido en el mapa: la latitud y la longitud de cada punto van juntas"),
    COORDENADAS_FUERA_DE_RANGO(TipoError.SOLICITUD_INVALIDA,
            "Las coordenadas del recorrido están fuera del rango válido");

    private final TipoError tipo;
    private final String mensaje;

    ErrorAlquiler(TipoError tipo, String mensaje) {
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