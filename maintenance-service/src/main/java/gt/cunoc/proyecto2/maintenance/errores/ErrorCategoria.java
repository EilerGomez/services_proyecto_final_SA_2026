package gt.cunoc.proyecto2.maintenance.errores;

public enum ErrorCategoria implements CodigoError {

    CATEGORIA_NO_EXISTE(TipoError.NO_ENCONTRADO, "La categoría de gasto no existe"),
    CATEGORIA_DUPLICADA(TipoError.CONFLICTO, "Ya existe una categoría de gasto con el nombre %s");

    private final TipoError tipo;
    private final String mensaje;

    ErrorCategoria(TipoError tipo, String mensaje) {
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
