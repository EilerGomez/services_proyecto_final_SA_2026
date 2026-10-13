package gt.cunoc.proyecto2.maintenance.errores;

import gt.cunoc.proyecto2.comun.errores.NegocioException;

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
