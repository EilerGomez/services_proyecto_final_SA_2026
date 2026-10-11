package gt.cunoc.proyecto2.maintenance.errores;

public enum ErrorTaller implements CodigoError {

    SERVICIO_NO_EXISTE(TipoError.NO_ENCONTRADO, "El servicio de taller no existe"),
    BUS_NO_EXISTE(TipoError.NO_ENCONTRADO, "El bus no existe en esta sucursal"),
    BUS_INACTIVO(TipoError.REGLA_NEGOCIO, "El bus está inactivo y no admite servicios de taller");

    private final TipoError tipo;
    private final String mensaje;

    ErrorTaller(TipoError tipo, String mensaje) {
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
