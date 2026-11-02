package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Actualiza en la proyección los datos de una sucursal que cambió en Fleet.
 */
@Component
public class ManejadorSucursalActualizada implements ManejadorEvento<SucursalEvento> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorSucursalActualizada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.SUCURSAL_ACTUALIZADA.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<SucursalEvento> tipoPayload() {
        return SucursalEvento.class;
    }

    // Aplica nombre, coordenadas y estado; descarta el evento si llega con una versión anterior.
    @Override
    public void manejar(EventoRecibido<SucursalEvento> evento) {
        proyeccion.aplicarSucursal(evento.payload(), evento.aggregateVersion());
    }
}
