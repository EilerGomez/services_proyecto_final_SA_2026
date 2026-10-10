package gt.cunoc.proyecto2.reporting.eventos.manejadores.maintenance;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance.CategoriaGastoActualizada;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

@Component
public class ManejadorCategoriaGastoActualizada implements ManejadorEvento<CategoriaGastoActualizada> {

    private final ProyeccionReportingServicio proyeccion;

    public ManejadorCategoriaGastoActualizada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.CATEGORIA_GASTO_ACTUALIZADA.nombre();
    }

    @Override
    public Class<CategoriaGastoActualizada> tipoPayload() {
        return CategoriaGastoActualizada.class;
    }

    @Override
    public void manejar(EventoRecibido<CategoriaGastoActualizada> evento) {
        proyeccion.aplicarCategoriaActualizada(
                evento.payload().categoriaId(), evento.payload().nombre());
    }
}
