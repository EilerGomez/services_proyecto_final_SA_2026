package gt.cunoc.proyecto2.wallet.errores;

import gt.cunoc.proyecto2.comun.errores.NegocioException;

/**
 * Contrato de los enums de errores: el nombre de la constante es el código que recibe el cliente.
 * {@code excepcion(datos...)} rellena los %s del mensaje.
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
