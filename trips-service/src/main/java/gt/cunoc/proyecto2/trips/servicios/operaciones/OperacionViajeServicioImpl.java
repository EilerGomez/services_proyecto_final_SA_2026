package gt.cunoc.proyecto2.trips.servicios.operaciones;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarLlegadaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarSalidaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
import gt.cunoc.proyecto2.trips.eventos.PublicadorEventosTrips;
import gt.cunoc.proyecto2.trips.modelos.buses.VistaBusEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.AlquilerPrivadoRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.buses.VistaBusRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.LlegadaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.SalidaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.tarifas.VistaTarifaDepreciacionRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacionViajeServicioImpl implements OperacionViajeServicio {

    private final ViajeServicio viajes;
    private final ViajeRepositorio viajeRepositorio;
    private final SalidaViajeRepositorio salidas;
    private final LlegadaViajeRepositorio llegadas;
    private final VistaTarifaDepreciacionRepositorio tarifas;
    private final AlquilerPrivadoRepositorio alquileres;
    private final VistaBusRepositorio buses;
    private final PublicadorEventosTrips eventos;

    public OperacionViajeServicioImpl(ViajeServicio viajes, ViajeRepositorio viajeRepositorio,
                                      SalidaViajeRepositorio salidas, LlegadaViajeRepositorio llegadas,
                                      VistaTarifaDepreciacionRepositorio tarifas,
                                      AlquilerPrivadoRepositorio alquileres, VistaBusRepositorio buses,
                                      PublicadorEventosTrips eventos) {
        this.viajes = viajes;
        this.viajeRepositorio = viajeRepositorio;
        this.salidas = salidas;
        this.llegadas = llegadas;
        this.tarifas = tarifas;
        this.alquileres = alquileres;
        this.buses = buses;
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public ViajeDetalleRespuesta registrarSalida(UUID viajeId, RegistrarSalidaSolicitud solicitud,
                                                 UsuarioActual usuario) {
        ViajeEntidad viaje = viajes.obtenerPorId(viajeId);
        verificarPuedeOperar(viaje, usuario);
        verificarSinOtroViajeEnRuta(viaje);                                     // 409 bus o piloto ocupados
        verificarEsElMasProximo(viaje);                                         // 422 si hay uno anterior
        verificarIdaCompletada(viaje);                                          // 422 si la vuelta se adelanta
        verificarFechaDeSalida(viaje, solicitud.fechaHoraSalida());             // 400 si se adelanta
        verificarKilometraje(viaje, solicitud.kmInicial());                     // 422 si el odómetro baja
        viaje.iniciar();                                                        // 409 si ya salió
        SalidaViajeEntidad salida = salidas.saveAndFlush(solicitud.crearEntidad(viaje, usuario.usuarioId()));
        viaje = viajeRepositorio.saveAndFlush(viaje);
        eventos.viajeIniciado(viaje, salida);
        return viajes.detalle(viaje);
    }

    @Override
    @Transactional
    public ViajeDetalleRespuesta registrarLlegada(UUID viajeId, RegistrarLlegadaSolicitud solicitud,
                                                  UsuarioActual usuario) {
        ViajeEntidad viaje = viajes.obtenerPorId(viajeId);
        verificarPuedeOperar(viaje, usuario);
        viaje.completar();                                                      // 422 / 409 según el estado
        SalidaViajeEntidad salida = salidas.findById(viajeId)
                .orElseThrow(() -> ErrorOperacionViaje.VIAJE_NO_INICIADO.excepcion());
        LlegadaViajeEntidad llegada = llegadas.saveAndFlush(solicitud.crearEntidad(viaje, salida,
                tarifaVigente(Fechas.diaEnGuatemala(solicitud.fechaHoraLlegada())), usuario.usuarioId()));
        viaje = viajeRepositorio.saveAndFlush(viaje);
        if (viaje.esPrivado()) {
            alquileres.findById(viaje.getAlquilerId()).ifPresent(alquiler -> {
                alquiler.completar();
                alquileres.saveAndFlush(alquiler);
            });
        }
        eventos.viajeCompletado(viaje, salida, llegada);
        return viajes.detalle(viaje);
    }

    /* El chofer asignado, o un ADMIN_SUCURSAL de la sucursal del viaje. */
    private static void verificarPuedeOperar(ViajeEntidad viaje, UsuarioActual usuario) {
        if (viaje.getChoferId().equals(usuario.choferId())) {
            return;
        }
        if (usuario.tieneRol(Roles.ADMIN_SUCURSAL)) {
            usuario.verificarAccesoSucursal(viaje.getSucursalId());            // 403 SUCURSAL_AJENA
            return;
        }
        throw ErrorOperacionViaje.CHOFER_NO_ASIGNADO.excepcion();
    }

    /**
     * T14. El bus y el piloto no pueden arrancar un viaje si vienen EN_RUTA en otro: físicamente no
     * están en dos lados. En la llegada no hace falta revisarlo, porque con esta regla un bus nunca
     * llega a tener dos viajes en ruta.
     */
    private void verificarSinOtroViajeEnRuta(ViajeEntidad viaje) {
        if (viajeRepositorio.existsByBusIdAndEstadoAndIdNot(viaje.getBusId(), EstadoViaje.EN_RUTA, viaje.getId())) {
            throw ErrorOperacionViaje.BUS_EN_RUTA.excepcion();
        }
        if (viajeRepositorio.existsByChoferIdAndEstadoAndIdNot(viaje.getChoferId(), EstadoViaje.EN_RUTA,
                viaje.getId())) {
            throw ErrorOperacionViaje.CHOFER_EN_RUTA.excepcion();
        }
    }

    /**
     * T14. Los viajes se arrancan en orden: si el piloto o el bus tienen otro viaje programado que
     * sale antes, ese va primero. Así el piloto solo opera el viaje que le toca y no deja atrás
     * viajes sin registrar, que es lo que luego descuadra los reportes.
     */
    private void verificarEsElMasProximo(ViajeEntidad viaje) {
        if (viajeRepositorio.existeOtroAntesDeEste(viaje.getChoferId(), viaje.getBusId(), viaje.getId(),
                EstadoViaje.PROGRAMADO, viaje.getFechaSalidaProgramada())) {
            throw ErrorOperacionViaje.HAY_UN_VIAJE_ANTERIOR.excepcion();
        }
    }

    /**
     * T14. El viaje de vuelta no sale hasta que su ida esté COMPLETADA: es el mismo bus haciendo el
     * mismo recorrido al revés. Si la ida se eliminó, el regreso queda suelto y opera por su cuenta.
     */
    private void verificarIdaCompletada(ViajeEntidad viaje) {
        if (!viaje.esRegreso() || viaje.getViajeIdaId() == null) {
            return;
        }
        boolean idaCompletada = viajeRepositorio.findById(viaje.getViajeIdaId())
                .map(ida -> ida.getEstado() == EstadoViaje.COMPLETADO)
                .orElse(true);
        if (!idaCompletada) {
            throw ErrorOperacionViaje.IDA_SIN_COMPLETAR.excepcion();
        }
    }

    /**
     * T14. El viaje no puede salir antes de la hora a la que estaba programado. Después sí: un bus
     * se puede atrasar, y esa es justamente la hora real que se registra.
     */
    private static void verificarFechaDeSalida(ViajeEntidad viaje, OffsetDateTime fechaHoraSalida) {
        if (fechaHoraSalida.isBefore(viaje.getFechaSalidaProgramada())) {
            throw ErrorOperacionViaje.SALIDA_ANTES_DE_LO_PROGRAMADO.excepcion(
                    Fechas.enGuatemala(viaje.getFechaSalidaProgramada()));
        }
    }

    /**
     * T14. El odómetro no camina para atrás: el kilometraje inicial no puede ser menor al que ya
     * trae el bus. Se toma el mayor entre el que manda Fleet (que lo sube con cada viaje completado)
     * y la última salida registrada aquí, por si ese evento todavía viene en camino.
     */
    private void verificarKilometraje(ViajeEntidad viaje, BigDecimal kmInicial) {
        BigDecimal actual = kilometrajeDelBus(viaje.getBusId());
        if (kmInicial.compareTo(actual) < 0) {
            throw ErrorOperacionViaje.KM_MENOR_AL_DEL_BUS.excepcion(kmInicial, actual);
        }
    }

    private BigDecimal kilometrajeDelBus(UUID busId) {
        BigDecimal deFleet = buses.findById(busId)
                .map(VistaBusEntidad::getKilometrajeActual)
                .orElse(BigDecimal.ZERO);
        BigDecimal deLaUltimaSalida = salidas.ultimoKilometrajeDelBus(busId);
        return deLaUltimaSalida == null ? deFleet : deFleet.max(deLaUltimaSalida);
    }

    private BigDecimal tarifaVigente(LocalDate fecha) {
        return tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(fecha)
                .orElseThrow(() -> ErrorOperacionViaje.SIN_TARIFA_VIGENTE.excepcion(fecha))
                .getMontoPorKm();
    }
}