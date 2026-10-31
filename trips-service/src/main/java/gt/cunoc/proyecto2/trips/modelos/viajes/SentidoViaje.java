package gt.cunoc.proyecto2.trips.modelos.viajes;

/**
 * Hacia dónde va un viaje regular dentro de su ruta (columna viajes.sentido, V17).
 *
 * <p>La ruta es una sola: el viaje de vuelta comparte ruta_id con el de ida y lo único que cambia es
 * el sentido. Al mostrarlo, un REGRESO se lee al revés (destino → origen), y de eso se encarga
 * {@code NombresViaje}, así que el resto del sistema —Sales, Reporting, los correos— ve el recorrido
 * ya en el orden correcto sin saber nada de esta columna.</p>
 *
 * <p>Un viaje PRIVADO siempre es IDA: nace de un alquiler, que lleva su propio retorno.</p>
 */
public enum SentidoViaje {
    /** De la sucursal de origen de la ruta a la de destino. */
    IDA,
    /** El mismo recorrido al revés: sale del destino de la ruta y vuelve al origen. */
    REGRESO;

    public boolean esRegreso() {
        return this == REGRESO;
    }
}
