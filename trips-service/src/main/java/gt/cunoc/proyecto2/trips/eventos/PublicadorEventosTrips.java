package gt.cunoc.proyecto2.trips.eventos;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.trips.dtos.rutas.RutaRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.eventos.publicados.alquileres.AlquilerAsignado;
import gt.cunoc.proyecto2.trips.eventos.publicados.alquileres.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.trips.eventos.publicados.rutas.RutaEliminada;
import gt.cunoc.proyecto2.trips.eventos.publicados.rutas.RutaEvento;
import gt.cunoc.proyecto2.trips.eventos.publicados.viajes.ViajeCompletado;
import gt.cunoc.proyecto2.trips.eventos.publicados.viajes.ViajeEliminado;
import gt.cunoc.proyecto2.trips.eventos.publicados.viajes.ViajeEvento;
import gt.cunoc.proyecto2.trips.eventos.publicados.viajes.ViajeIniciado;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Los eventos que publica Trips, con su tipo de agregado, aggregateId, aggregateVersion y businessDate.
 * Los servicios llaman a estos métodos en lugar de escribir nombres de eventos a mano.
 */
@Component
public class PublicadorEventosTrips {

    private final PublicadorEventos publicador;

    public PublicadorEventosTrips(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    // ----- Rutas -----

    public void rutaCreada(RutaRespuesta ruta) {
        publicar(TipoAgregado.RUTA, ruta.rutaId(), ruta.version(), TipoEvento.RUTA_CREADA,
                RutaEvento.desde(ruta), Fechas.hoy());
    }

    public void rutaActualizada(RutaRespuesta ruta) {
        publicar(TipoAgregado.RUTA, ruta.rutaId(), ruta.version(), TipoEvento.RUTA_ACTUALIZADA,
                RutaEvento.desde(ruta), Fechas.hoy());
    }

    public void rutaEliminada(RutaEntidad ruta) {
        publicar(TipoAgregado.RUTA, ruta.getId(), siguienteVersion(ruta.getVersion()), TipoEvento.RUTA_ELIMINADA,
                new RutaEliminada(ruta.getId()), Fechas.hoy());
    }

    // ----- Viajes -----

    /** T10 (REGULAR) y T22 (PRIVADO). */
    public void viajeProgramado(ViajeRespuesta viaje) {
        publicar(TipoAgregado.VIAJE, viaje.viajeId(), viaje.version(), TipoEvento.VIAJE_PROGRAMADO,
                ViajeEvento.desde(viaje), Fechas.diaEnGuatemala(viaje.fechaSalidaProgramada()));
    }

    public void viajeActualizado(ViajeRespuesta viaje) {
        publicar(TipoAgregado.VIAJE, viaje.viajeId(), viaje.version(), TipoEvento.VIAJE_ACTUALIZADO,
                ViajeEvento.desde(viaje), Fechas.diaEnGuatemala(viaje.fechaSalidaProgramada()));
    }

    public void viajeEliminado(ViajeEntidad viaje, String motivo) {
        publicar(TipoAgregado.VIAJE, viaje.getId(), siguienteVersion(viaje.getVersion()),
                TipoEvento.VIAJE_ELIMINADO, ViajeEliminado.desde(viaje, motivo), Fechas.hoy());
    }

    public void viajeIniciado(ViajeEntidad viaje, SalidaViajeEntidad salida) {
        publicar(TipoAgregado.VIAJE, viaje.getId(), viaje.getVersion(), TipoEvento.VIAJE_INICIADO,
                ViajeIniciado.desde(viaje, salida), Fechas.diaEnGuatemala(salida.getFechaHoraSalida()));
    }

    public void viajeCompletado(ViajeEntidad viaje, SalidaViajeEntidad salida, LlegadaViajeEntidad llegada) {
        publicar(TipoAgregado.VIAJE, viaje.getId(), viaje.getVersion(), TipoEvento.VIAJE_COMPLETADO,
                ViajeCompletado.desde(viaje, salida, llegada), Fechas.diaEnGuatemala(llegada.getFechaHoraLlegada()));
    }

    // ----- Alquileres -----

    public void cotizacionAlquilerConfirmada(AlquilerPrivadoEntidad alquiler) {
        publicar(TipoAgregado.ALQUILER_PRIVADO, alquiler.getId(), alquiler.getVersion(),
                TipoEvento.COTIZACION_ALQUILER_CONFIRMADA, CotizacionAlquilerConfirmada.desde(alquiler),
                Fechas.hoy());
    }

    /** T22: el alquiler pagado ya quedó con bus y chofer (Notification avisa al cliente). */
    public void alquilerAsignado(AlquilerPrivadoEntidad alquiler, ViajeRespuesta viaje) {
        publicar(TipoAgregado.ALQUILER_PRIVADO, alquiler.getId(), alquiler.getVersion(),
                TipoEvento.ALQUILER_ASIGNADO, AlquilerAsignado.desde(alquiler, viaje), Fechas.hoy());
    }

    private static Integer siguienteVersion(Integer version) {
        return version == null ? null : version + 1;
    }

    private void publicar(TipoAgregado agregado, UUID agregadoId, Integer version, TipoEvento evento,
                          Object payload, LocalDate fecha) {
        publicador.publicar(agregado.nombre(), agregadoId, version == null ? null : version.longValue(),
                evento.nombre(), payload, fecha);
    }
}