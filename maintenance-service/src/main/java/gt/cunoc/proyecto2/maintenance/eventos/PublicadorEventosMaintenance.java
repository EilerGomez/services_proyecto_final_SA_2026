package gt.cunoc.proyecto2.maintenance.eventos;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.maintenance.eventos.publicados.CategoriaGastoActualizada;
import gt.cunoc.proyecto2.maintenance.eventos.publicados.GastoTallerRegistrado;
import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventosMaintenance {

    private final PublicadorEventos publicador;

    public PublicadorEventosMaintenance(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    public void gastoRegistrado(ServicioTallerEntidad servicio) {
        publicador.publicar(TipoAgregado.SERVICIO_TALLER.nombre(), servicio.getId(),
                TipoEvento.GASTO_TALLER_REGISTRADO.nombre(),
                GastoTallerRegistrado.desde(servicio), servicio.getFechaServicio());
    }

    public void categoriaActualizada(CategoriaEntidad categoria) {
        publicador.publicar(TipoAgregado.CATEGORIA_GASTO.nombre(), categoria.getId(),
                TipoEvento.CATEGORIA_GASTO_ACTUALIZADA.nombre(),
                CategoriaGastoActualizada.desde(categoria), LocalDate.now());
    }
}
