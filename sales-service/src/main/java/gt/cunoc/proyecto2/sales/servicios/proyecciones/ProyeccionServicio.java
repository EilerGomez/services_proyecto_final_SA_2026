package gt.cunoc.proyecto2.sales.servicios.proyecciones;

import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeEvento;
import java.util.UUID;

/** Copias locales que llenan los eventos: vista_viajes (con sus asientos) y vista_clientes. */
public interface ProyeccionServicio {

    /** ViajeProgramado / ViajeActualizado (solo REGULAR): upsert del viaje y crea los asientos que falten. */
    void guardarViaje(ViajeEvento viaje, Long version);

    /** ViajeIniciado: el viaje pasa a EN_RUTA y deja de venderse. */
    void iniciarViaje(UUID viajeId, Long version);

    /** ViajeEliminado: borra el viaje y sus asientos (después de compensar las ventas). */
    void eliminarViaje(UUID viajeId, Long version);

    /** ClienteRegistrado / PerfilActualizado: upsert del cliente (NIT, nombre y correo). */
    void guardarCliente(UUID usuarioId, String nit, String nombre, String correo);
}
