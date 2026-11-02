package gt.cunoc.proyecto2.reporting.eventos.manejadores.maintenance;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance.CategoriaGastoActualizada;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import org.springframework.stereotype.Component;

/**
 * Actualiza el nombre de una categoría de gasto en todos los gastos de taller ya registrados.
 */
@Component
public class ManejadorCategoriaGastoActualizada implements ManejadorEvento<CategoriaGastoActualizada> {

    private final ProyeccionReportingServicio proyeccion;

    // Recibe el servicio que actualiza las tablas del modelo de lectura.
    public ManejadorCategoriaGastoActualizada(ProyeccionReportingServicio proyeccion) {
        this.proyeccion = proyeccion;
    }

    // Nombre del evento que atiende este manejador.
    @Override
    public String tipoEvento() {
        return TipoEvento.CATEGORIA_GASTO_ACTUALIZADA.nombre();
    }

    // Clase a la que se deserializa el contenido del evento.
    @Override
    public Class<CategoriaGastoActualizada> tipoPayload() {
        return CategoriaGastoActualizada.class;
    }

    // Propaga la nueva descripción de la categoría a los gastos que la usaban.
    @Override
    public void manejar(EventoRecibido<CategoriaGastoActualizada> evento) {
        proyeccion.aplicarCategoriaActualizada(
                evento.payload().categoriaId(), evento.payload().nombre());
    }
}
