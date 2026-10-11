package gt.cunoc.proyecto2.sales.eventos;

/** Agregados de Sales: el "dueño" de cada evento (columna eventos_salida.tipo_agregado). */
public enum TipoAgregado {

    VENTA("Venta"),
    ORDEN_ALQUILER("OrdenAlquiler");

    private final String nombre;

    TipoAgregado(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
