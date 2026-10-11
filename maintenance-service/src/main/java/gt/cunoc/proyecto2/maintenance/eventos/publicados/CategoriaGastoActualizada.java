package gt.cunoc.proyecto2.maintenance.eventos.publicados;

import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import java.util.UUID;

/** Payload de CategoriaGastoActualizada. Consume: Reporting. */
public record CategoriaGastoActualizada(
        UUID categoriaId,
        String nombre,
        boolean activa) {

    public static CategoriaGastoActualizada desde(CategoriaEntidad categoria) {
        return new CategoriaGastoActualizada(
                categoria.getId(), categoria.getNombre(), categoria.isActiva());
    }
}
