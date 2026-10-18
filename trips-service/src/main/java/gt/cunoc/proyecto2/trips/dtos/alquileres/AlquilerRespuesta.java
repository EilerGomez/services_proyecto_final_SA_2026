package gt.cunoc.proyecto2.trips.dtos.alquileres;

import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Respuesta de T16–T21. viajeId/viaje: el viaje PRIVADO asignado (null hasta T22). */
public record AlquilerRespuesta(    
        UUID alquilerId,
        UUID sucursalId,
        CanalAlquiler canal,
        UUID clienteUsuarioId,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        String origen,
        String destino,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        int cantidadPasajeros,
        BigDecimal kmEstimados,
        BigDecimal precioEstimado,
        BigDecimal precioConfirmado,
        UUID confirmadoPor,
        EstadoAlquiler estado,
        String motivoCancelacion,
        LocalDate fechaSolicitud,
        UUID viajeId,
        // Viaje PRIVADO ya asignado (T22): bus, chofer y fechas. Null mientras no esté asignado.
        ViajeRespuesta viaje,
        Integer version) {

    public AlquilerRespuesta(AlquilerPrivadoEntidad alquiler, UUID viajeId) {
        this(alquiler, viajeId, null);
    }

    /**
     * Con el viaje PRIVADO ya asignado (T22). Se incluye el viaje completo para que las pantallas
     * muestren bus, chofer y fechas en lugar del UUID suelto.
     */
    public AlquilerRespuesta(AlquilerPrivadoEntidad alquiler, UUID viajeId, ViajeRespuesta viaje) {
        this(alquiler.getId(),
                alquiler.getSucursalId(),
                alquiler.getCanal(),
                alquiler.getClienteUsuarioId(),
                alquiler.getClienteNit(),
                alquiler.getClienteNombre(),
                alquiler.getClienteCorreo(),
                alquiler.getOrigen(),
                alquiler.getDestino(),
                Fechas.enGuatemala(alquiler.getFechaSalida()),
                Fechas.enGuatemala(alquiler.getFechaRetorno()),
                alquiler.getCantidadPasajeros(),
                alquiler.getKmEstimados(),
                alquiler.getPrecioEstimado(),
                alquiler.getPrecioConfirmado(),
                alquiler.getConfirmadoPor(),
                alquiler.getEstado(),
                alquiler.getMotivoCancelacion(),
                alquiler.getFechaSolicitud(),
                viajeId,
                viaje,
                alquiler.getVersion());
    }
}