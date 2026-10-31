package gt.cunoc.proyecto2.trips.dtos.viajes;

import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.SentidoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Cuerpo de · POST /trips/viajes (viaje REGULAR).
 * Las fechas las elige el usuario, con zona: "2026-10-25T06:00:00-06:00".
 */
public record NuevoViajeSolicitud(
        @NotNull UUID rutaId,
        @NotNull UUID busId,
        @NotNull UUID choferId,
        @NotNull OffsetDateTime fechaSalidaProgramada,
        @NotNull OffsetDateTime fechaLlegadaEstimada,
        /** Si el piloto de la ida puede vender boletos presenciales. Null se toma como false. */
        Boolean ventaPorChofer,
        /**
         * El viaje de vuelta por la misma ruta, o null si solo se programa la ida. Cuando viene, los
         * dos viajes se crean juntos: o entran los dos o no entra ninguno.
         */
        @Valid ViajeRegresoSolicitud regreso) {

    /** asignacion: bus, chofer y horario ya validados con R1 y choques de horario. */
    public ViajeEntidad crearEntidad(RutaEntidad ruta, AsignacionViaje asignacion, UUID creadoPor) {
        return ViajeEntidad.programarRegular(ruta, asignacion, creadoPor, SentidoViaje.IDA,
                ventaPorChoferActiva());
    }

    public boolean ventaPorChoferActiva() {
        return Boolean.TRUE.equals(ventaPorChofer);
    }

    public boolean pidioRegreso() {
        return regreso != null;
    }
}
