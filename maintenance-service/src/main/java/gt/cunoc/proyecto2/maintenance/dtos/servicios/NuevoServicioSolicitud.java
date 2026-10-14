package gt.cunoc.proyecto2.maintenance.dtos.servicios;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.DetalleServicioEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NuevoServicioSolicitud(
        @NotNull
        UUID busId,

        @NotNull
        LocalDate fechaServicio,

        @Size(max = 150)
        String taller,

        @Size(max = 500)
        String observaciones,

        @NotNull
        @Size(min = 1)
        List<@Valid @NotNull LineaServicioSolicitud> lineas) {

    public NuevoServicioSolicitud {
        taller = taller == null || taller.isBlank() ? null : taller.strip();
        observaciones = observaciones == null || observaciones.isBlank() ? null : observaciones.strip();
    }

    public ServicioTallerEntidad crearEntidad(VistaBusEntidad bus, UsuarioActual usuario) {
        ServicioTallerEntidad servicio = new ServicioTallerEntidad();
        servicio.setBusId(bus.getBusId());
        servicio.setSucursalId(bus.getSucursalId());
        servicio.setFechaServicio(fechaServicio);
        servicio.setTaller(taller);
        servicio.setObservaciones(observaciones);
        servicio.setRegistradoPor(usuario.usuarioId());
        return servicio;
    }

    public DetalleServicioEntidad crearDetalle(LineaServicioSolicitud linea, CategoriaEntidad c) {
        DetalleServicioEntidad detalle = new DetalleServicioEntidad();
        detalle.setCategoriaId(linea.categoriaId());
        detalle.setCategoria(c);
        detalle.setDescripcion(linea.descripcion());
        detalle.setTipo(linea.tipo());
        detalle.setMonto(linea.monto());
        return detalle;
    }
}
