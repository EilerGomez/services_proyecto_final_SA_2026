package gt.cunoc.proyecto2.maintenance.dtos.categorias;

import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import java.util.UUID;

public record CategoriaRespuesta(
        UUID categoriaId,
        String nombre,
        String descripcion,
        boolean activa) {

    public CategoriaRespuesta(CategoriaEntidad categoria) {
        this(categoria.getId(), categoria.getNombre(), categoria.getDescripcion(), categoria.isActiva());
    }
}
