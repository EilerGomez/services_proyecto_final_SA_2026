package gt.cunoc.proyecto2.maintenance.dtos.categorias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NuevaCategoriaSolicitud(
        @NotBlank @Size(max = 60) String nombre,
        @Size(max = 255) String descripcion) {

    public NuevaCategoriaSolicitud {
        nombre = nombre == null ? null : nombre.strip();
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
    }
}
