package gt.cunoc.proyecto2.sales.errores;

import gt.cunoc.proyecto2.comun.errores.NegocioException;

/**
 * Lo implementan los enums de errores de cada módulo (ErrorViaje, ErrorVenta, ...).
 * El nombre de la constante es el código que recibe el frontend.
 */
public interface CodigoError {

    String name();

    TipoError tipo();

    String mensaje();

    default String codigo() {
        return name();
    }

    default NegocioException excepcion(Object... datos) {
        String texto = datos.length == 0 ? mensaje() : String.format(mensaje(), datos);
        return tipo().crear(codigo(), texto);
    }
}