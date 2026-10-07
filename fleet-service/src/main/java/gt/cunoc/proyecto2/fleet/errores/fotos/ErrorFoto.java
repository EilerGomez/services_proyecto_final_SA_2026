package gt.cunoc.proyecto2.fleet.errores.fotos;

import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import gt.cunoc.proyecto2.fleet.errores.TipoError;

/**
 * Errores de fotos.
 * ARCHIVO_INVALIDO: el archivo que se sube en F22. FOTO_INVALIDA: una fotoUrl del JSON que no es del bucket.
 */
public enum ErrorFoto implements CodigoError {

    ARCHIVO_INVALIDO(TipoError.SOLICITUD_INVALIDA, "La foto es obligatoria: JPG, PNG o WEBP de máximo 2 MB"),
    FOTO_INVALIDA(TipoError.SOLICITUD_INVALIDA, "La foto debe subirse primero con POST /fleet/archivos/fotos"),
    FOTO_OBLIGATORIA(TipoError.SOLICITUD_INVALIDA, "La foto es obligatoria"),
    ALMACENAMIENTO_NO_DISPONIBLE(TipoError.SERVICIO_NO_DISPONIBLE, "No se pudo guardar la foto; intente de nuevo");

    private final TipoError tipo;
    private final String mensaje;

    ErrorFoto(TipoError tipo, String mensaje) {
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