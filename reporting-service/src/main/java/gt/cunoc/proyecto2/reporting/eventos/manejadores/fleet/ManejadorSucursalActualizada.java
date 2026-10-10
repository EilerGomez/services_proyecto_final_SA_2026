package gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorSucursalActualizada implements ManejadorEvento<SucursalEvento> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorSucursalActualizada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.SUCURSAL_ACTUALIZADA.nombre();
    }

    @Override
    public Class<SucursalEvento> tipoPayload() {
        return SucursalEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<SucursalEvento> evento) {
        proyeccion.aplicarSucursal(evento.payload(), evento.aggregateVersion());
    }
}
