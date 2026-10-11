package gt.cunoc.proyecto2.maintenance.eventos;

public enum TipoAgregado {

    SERVICIO_TALLER("ServicioTaller"),
    CATEGORIA_GASTO("CategoriaGasto");

    private final String nombre;

    TipoAgregado(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
