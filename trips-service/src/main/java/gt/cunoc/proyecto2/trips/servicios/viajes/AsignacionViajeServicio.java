package gt.cunoc.proyecto2.trips.servicios.viajes;

import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Validación de bus, chofer y horario de un viaje con R1 (Fleet) y con los viajes ya programados.
 * La usan T10 (regular), T11 (cambio) y T22 (privado).
 */
public interface AsignacionViajeServicio {

    /*
     * Viaje nuevo de la sucursal indicada
     * Fleet sin responder o circuito abierto → 503 FLEET_NO_DISPONIBLE.
     */
    AsignacionViaje validarNueva(UUID sucursalId, UUID busId, UUID choferId,
                                 OffsetDateTime salida, OffsetDateTime llegada);

    /*
     * T11: mismas reglas, pero R1a solo si cambia el bus y R1b solo si cambia el chofer o el día de salida
     * (la licencia se revisa con la fecha nueva). Si el chofer no cambia, se conserva su salario copiado.
     */
    AsignacionViaje validarCambio(ViajeEntidad viaje, UUID busId, UUID choferId,
                                  OffsetDateTime salida, OffsetDateTime llegada);
}