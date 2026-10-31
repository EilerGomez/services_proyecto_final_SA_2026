package gt.cunoc.proyecto2.trips.dtos.viajes;

import com.fasterxml.jackson.annotation.JsonInclude;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.SentidoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Respuesta de T8–T13. Las fechas salen con la hora de Guatemala (-06:00). */
public record ViajeRespuesta(
        UUID viajeId,
        TipoViaje tipo,
        UUID rutaId,
        UUID alquilerId,
        UUID sucursalId,
        String origenNombre,
        String destinoNombre,
        /*
         * Solo en un viaje PRIVADO: los puntos que el cliente marcó al pedir el alquiler, para
         * dibujar su recorrido en el detalle. En un REGULAR no van (la ruta trae sus sucursales).
         */
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal origenLatitud,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal origenLongitud,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal destinoLatitud,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal destinoLongitud,
        UUID busId,
        String busPlaca,
        /** Foto del bus (de vista_buses): el pasajero la ve al comprar, sin necesitar sesión. */
        String busFotoUrl,
        /** Odómetro del bus: el piso del kilometraje inicial al registrar la salida. */
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal busKilometrajeActual,
        UUID choferId,
        String choferNombre,
        /** Cuenta del piloto en Identity: con ella Notification le avisa que tiene un viaje nuevo. */
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID choferUsuarioId,
        int capacidad,
        BigDecimal precioBoleto,
        OffsetDateTime fechaSalidaProgramada,
        OffsetDateTime fechaLlegadaEstimada,
        /*
         * Solo en un viaje PRIVADO de ida y vuelta: cuándo pidió el cliente el regreso. No es lo
         * mismo que la llegada estimada, que es la del trayecto de ida.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime fechaRetorno,
        EstadoViaje estado,
        /** IDA o REGRESO: en un REGRESO el origen y el destino ya vienen invertidos. */
        SentidoViaje sentido,
        /** El piloto asignado puede vender boletos presenciales de este viaje. */
        boolean ventaPorChofer,
        Integer version) {

    public ViajeRespuesta(ViajeEntidad viaje, NombresViaje nombres) {
        this(viaje.getId(),
                viaje.getTipo(),
                viaje.getRutaId(),
                viaje.getAlquilerId(),
                viaje.getSucursalId(),
                nombres.origenNombre(viaje),
                nombres.destinoNombre(viaje),
                nombres.origenLatitud(viaje),
                nombres.origenLongitud(viaje),
                nombres.destinoLatitud(viaje),
                nombres.destinoLongitud(viaje),
                viaje.getBusId(),
                nombres.placa(viaje),
                nombres.fotoBus(viaje),
                nombres.kilometrajeBus(viaje),
                viaje.getChoferId(),
                nombres.chofer(viaje),
                nombres.choferUsuarioId(viaje),
                viaje.getCapacidadBus(),
                viaje.getPrecioBoleto(),
                Fechas.enGuatemala(viaje.getFechaSalidaProgramada()),
                Fechas.enGuatemala(viaje.getFechaLlegadaEstimada()),
                Fechas.enGuatemala(nombres.fechaRetorno(viaje)),
                viaje.getEstado(),
                viaje.getSentido(),
                viaje.isVentaPorChofer(),
                viaje.getVersion());
    }
}