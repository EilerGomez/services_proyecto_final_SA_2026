package gt.cunoc.proyecto2.trips.eventos;

/** Agregados de Trips: el "dueño" de cada evento (columna eventos_salida.tipo_agregado). */
public enum TipoAgregado {

    RUTA("Ruta"),
    VIAJE("Viaje"),
    ALQUILER_PRIVADO("AlquilerPrivado");

    private final String nombre;

    TipoAgregado(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}