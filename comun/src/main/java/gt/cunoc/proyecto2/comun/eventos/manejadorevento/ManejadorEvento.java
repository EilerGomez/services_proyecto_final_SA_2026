package gt.cunoc.proyecto2.comun.eventos.manejadorevento;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;

/**
 * Implementar como @Component en el servicio consumidor, uno por tipo de evento.
 * {@link ConsumidorEventos} lo invoca dentro de una transacción idempotente:
 * si el evento ya se procesó, no se vuelve a llamar.
 *
 * <pre>
 * &#64;Component
 * class ViajeProgramadoHandler implements ManejadorEvento&lt;ViajeProgramado&gt; {
 *     public String tipoEvento() { return "TripScheduled"; }
 *     public Class&lt;ViajeProgramado&gt; tipoPayload() { return ViajeProgramado.class; }
 *     public void manejar(EventoRecibido&lt;ViajeProgramado&gt; e) { ... }
 * }
 * </pre>
 */
public interface ManejadorEvento<T> {

    /** Nombre del evento, igual a la routing key (p. ej. "TripScheduled"). */
    String tipoEvento();

    /** Record del payload, con solo los campos que este servicio usa. */
    Class<T> tipoPayload();

    /** Lógica del consumidor. Si lanza una excepción, se reintenta y al final va a la DLQ. */
    void manejar(EventoRecibido<T> evento);
}
