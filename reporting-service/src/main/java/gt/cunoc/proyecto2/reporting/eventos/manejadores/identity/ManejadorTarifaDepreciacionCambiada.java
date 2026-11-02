package gt.cunoc.proyecto2.reporting.eventos.manejadores.identity;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Guarda la tarifa de depreciación que Identity cambia para calcular ese gasto en los viajes.
 */
@Component
public class ManejadorTarifaDepreciacionCambiada implements ManejadorEvento<TarifaDepreciacionCambiada> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorTarifaDepreciacionCambiada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.TARIFA_DEPRECIACION_CAMBIADA.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<TarifaDepreciacionCambiada> tipoPayload() {
        return TarifaDepreciacionCambiada.class;
    }

    // Registra la tarifa; si ya existía o llega incompleta, el servicio la ignora sin fallar.
    @Override
    public void manejar(EventoRecibido<TarifaDepreciacionCambiada> evento) {
        proyeccion.aplicarTarifa(evento.payload());
    }
}
