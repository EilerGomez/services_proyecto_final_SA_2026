package gt.cunoc.proyecto2.maintenance.eventos;

public enum TipoEvento {

    // ----- Consume Maintenance (cola maintenance.q) -----
    BUS_CREADO("BusCreado"),
    BUS_ACTUALIZADO("BusActualizado"),
    BUS_DESACTIVADO("BusDesactivado"),

    // ----- Publica Maintenance (exchange maintenance.eventos) -----
    GASTO_TALLER_REGISTRADO("GastoTallerRegistrado"),
    CATEGORIA_GASTO_ACTUALIZADA("CategoriaGastoActualizada");

    private final String nombre;

    TipoEvento(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
