package gt.cunoc.proyecto2.trips.servicios.viajes;

import gt.cunoc.proyecto2.trips.clientes.fleet.ClienteFleet;
import gt.cunoc.proyecto2.trips.dtos.fleet.ChoferFleetRespuesta;
import gt.cunoc.proyecto2.trips.dtos.fleet.DisponibilidadBusRespuesta;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AsignacionViajeServicioImpl implements AsignacionViajeServicio {

    /** Un viaje nuevo todavía no tiene id: este no coincide con ninguno al buscar choques. */
    private static final UUID SIN_VIAJE = new UUID(0L, 0L);

    private final ClienteFleet fleet;
    private final ViajeRepositorio viajes;

    public AsignacionViajeServicioImpl(ClienteFleet fleet, ViajeRepositorio viajes) {
        this.fleet = fleet;
        this.viajes = viajes;
    }

    @Override
    public AsignacionViaje validarNueva(UUID sucursalId, UUID busId, UUID choferId,
                                        OffsetDateTime salida, OffsetDateTime llegada) {
        AsignacionViaje.verificarFechas(salida, llegada);             // antes de llamar a Fleet
        
        DisponibilidadBusRespuesta bus = validarBus(sucursalId, busId);
        
        ChoferFleetRespuesta chofer = validarChofer(sucursalId, choferId, Fechas.diaEnGuatemala(salida));
        
        verificarHorario(SIN_VIAJE, busId, choferId, salida, llegada);
        
        return new AsignacionViaje(busId, bus.capacidadPasajeros(), choferId, chofer.salarioBaseViaje(),
                salida, llegada);
    }

    @Override
    public AsignacionViaje validarCambio(ViajeEntidad viaje, UUID busId, UUID choferId,
                                         OffsetDateTime salida, OffsetDateTime llegada) {
        
        AsignacionViaje.verificarFechas(salida, llegada);
        
        int capacidad = viaje.getCapacidadBus();
        
        if (!busId.equals(viaje.getBusId())) {
            
            capacidad = validarBus(viaje.getSucursalId(), busId).capacidadPasajeros();
            if (capacidad < viaje.getCapacidadBus()) {          // antes de seguir llamando a Fleet
                throw ErrorViaje.CAPACIDAD_MENOR.excepcion(capacidad, viaje.getCapacidadBus());
            }
            
        }
        
        BigDecimal salario = viaje.getSalarioBaseChofer();
        
        LocalDate dia = Fechas.diaEnGuatemala(salida);
        
        boolean cambiaChofer = !choferId.equals(viaje.getChoferId());
        
        if (cambiaChofer || !dia.equals(Fechas.diaEnGuatemala(viaje.getFechaSalidaProgramada()))) {
            
            ChoferFleetRespuesta chofer = validarChofer(viaje.getSucursalId(), choferId, dia);
            
            if (cambiaChofer) {
                salario = chofer.salarioBaseViaje();
            }
            
        }
        
        verificarHorario(viaje.getId(), busId, choferId, salida, llegada);
        
        return new AsignacionViaje(busId, capacidad, choferId, salario, salida, llegada);
    }

    /** R1a. */
    private DisponibilidadBusRespuesta validarBus(UUID sucursalId, UUID busId) {
        
        DisponibilidadBusRespuesta bus = fleet.disponibilidadDelBus(busId)
                .orElseThrow(() -> ErrorViaje.BUS_NO_DISPONIBLE.excepcion("no existe en Flota"));
        
        if (!sucursalId.equals(bus.sucursalId())) {
            
            throw ErrorViaje.BUS_NO_DISPONIBLE.excepcion("el bus " + bus.placa() + " es de otra sucursal");
            
        }
        if (!bus.disponible()) {
            
            throw ErrorViaje.BUS_NO_DISPONIBLE.excepcion("el bus " + bus.placa() + " está " + bus.estado());
            
        }
        return bus;
    }

    /** R1b con la fecha del viaje. */
    private ChoferFleetRespuesta validarChofer(UUID sucursalId, UUID choferId, LocalDate fecha) {
        
        ChoferFleetRespuesta chofer = fleet.choferParaViaje(choferId, fecha)
                .orElseThrow(() -> ErrorViaje.CHOFER_NO_DISPONIBLE.excepcion("no existe en Flota"));
        
        if (!sucursalId.equals(chofer.sucursalId())) {
            throw ErrorViaje.CHOFER_NO_DISPONIBLE.excepcion(chofer.nombreCompleto() + " es de otra sucursal");
        }
        
        if (!chofer.esActivo()) {
            throw ErrorViaje.CHOFER_NO_DISPONIBLE.excepcion(chofer.nombreCompleto() + " está inactivo");
        }
        
        if (!chofer.licenciaVigente()) {
            throw ErrorViaje.LICENCIA_VENCIDA.excepcion(chofer.fechaVencimientoLicencia());
        }
        
        return chofer;
    }

    /** Dos viajes chocan si sus intervalos [salida, llegada estimada) se cruzan. */
    private void verificarHorario(UUID viajeId, UUID busId, UUID choferId,
                                  OffsetDateTime salida, OffsetDateTime llegada) {
        
        if (viajes.existeChoqueDeBus(busId, viajeId, EstadoViaje.ACTIVOS, salida, llegada)) {
            throw ErrorViaje.BUS_OCUPADO.excepcion();
        }
        
        if (viajes.existeChoqueDeChofer(choferId, viajeId, EstadoViaje.ACTIVOS, salida, llegada)) {
            throw ErrorViaje.CHOFER_OCUPADO.excepcion();
        }
    }
}