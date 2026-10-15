package gt.cunoc.proyecto2.fleet.servicios.buses;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.clientes.trips.ClienteTrips;
import gt.cunoc.proyecto2.fleet.dtos.buses.ActualizarBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.BusRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.CambiarEstadoBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.DisponibilidadBusRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.NuevoBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.trips.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.fleet.errores.buses.ErrorBus;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.buses.BusRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.fotos.AlmacenFotosServicio;
import gt.cunoc.proyecto2.fleet.utilidades.Fechas;
import gt.cunoc.proyecto2.fleet.utilidades.FiltroSucursal;
import gt.cunoc.proyecto2.fleet.utilidades.VerificadorVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusServicioImpl implements BusServicio {

    private final BusRepositorio repositorio;
    private final SucursalRepositorio sucursales;
    private final AlmacenFotosServicio fotos;
    private final ClienteTrips trips;
    private final PublicadorEventosFleet eventos;

    public BusServicioImpl(BusRepositorio repositorio, SucursalRepositorio sucursales,
                           AlmacenFotosServicio fotos, ClienteTrips trips, PublicadorEventosFleet eventos) {
        this.repositorio = repositorio;
        this.sucursales = sucursales;
        this.fotos = fotos;
        this.trips = trips;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusEntidad> listar(UUID sucursalId, EstadoBus estado, UsuarioActual usuario) {
        return repositorio.buscar(FiltroSucursal.resolver(usuario, sucursalId), estado);
    }

    @Override
    @Transactional(readOnly = true)
    public BusEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorBus.BUS_NO_EXISTE.excepcion());
    }

    @Override
    @Transactional(readOnly = true)
    public BusEntidad obtener(UUID id, UsuarioActual usuario) {
        BusEntidad bus = obtenerPorId(id);
        usuario.verificarAccesoSucursal(bus.getSucursalId());
        return bus;
    }

    @Override
    @Transactional
    public BusRespuesta crear(NuevoBusSolicitud solicitud, UsuarioActual usuario) {
        usuario.verificarAccesoSucursal(solicitud.sucursalId());
        verificarSucursalActiva(solicitud.sucursalId());
        fotos.verificarUrl(solicitud.fotoUrl());
        if (repositorio.existsByPlaca(solicitud.placa())) {
            throw ErrorBus.PLACA_EN_USO.excepcion(solicitud.placa());
        }

        BusEntidad bus = repositorio.saveAndFlush(solicitud.crearEntidad());
        eventos.busCreado(bus, solicitud.fechaRegistro());

        return new BusRespuesta(bus);
    }

    @Override
    @Transactional
    public BusRespuesta actualizar(UUID id, ActualizarBusSolicitud solicitud, UsuarioActual usuario) {
        BusEntidad bus = obtener(id, usuario);
        VerificadorVersion.verificar(bus.getVersion(), solicitud.version());
        fotos.verificarUrl(solicitud.fotoUrl());
        String fotoAnterior = bus.getFotoUrl();

        solicitud.aplicarA(bus);
        bus = repositorio.saveAndFlush(bus);
        eventos.busActualizado(bus, Fechas.hoy());
        // Foto reemplazada: la anterior se borra del almacenamiento cuando se confirma el cambio
        if (fotoAnterior != null && !fotoAnterior.equals(bus.getFotoUrl())) {
            fotos.eliminarAlConfirmar(fotoAnterior);
        }

        return new BusRespuesta(bus);
    }

    @Override
    @Transactional
    public BusRespuesta cambiarEstado(UUID id, CambiarEstadoBusSolicitud solicitud, UsuarioActual usuario) {
        BusEntidad bus = obtener(id, usuario);
        VerificadorVersion.verificar(bus.getVersion(), solicitud.version());

        EstadoBus nuevo = solicitud.estado();
        if (bus.getEstado() == nuevo) {
            return new BusRespuesta(bus);
        }
        bus.validarCambioDeEstado(nuevo);

        if (nuevo == EstadoBus.ACTIVO) {
            verificarSucursalActiva(bus.getSucursalId());
        }
        if (nuevo == EstadoBus.INACTIVO) {
            ViajesActivosRespuesta viajes = trips.viajesActivosDelBus(id);     // R3
            if (viajes.hayViajes()) {
                throw ErrorBus.BUS_CON_VIAJES_ACTIVOS.excepcion(viajes.cantidad());
            }
        }

        bus.cambiarEstado(nuevo);
        bus = repositorio.saveAndFlush(bus);

        if (nuevo == EstadoBus.INACTIVO) {
            eventos.busDesactivado(bus, solicitud.fecha());
        } else {
            eventos.busActualizado(bus, solicitud.fecha());
        }

        return new BusRespuesta(bus);
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadBusRespuesta consultarDisponibilidad(UUID id) {
        return new DisponibilidadBusRespuesta(obtenerPorId(id));
    }

    private void verificarSucursalActiva(UUID sucursalId) {
        SucursalEntidad sucursal = sucursales.findById(sucursalId)
                .orElseThrow(() -> ErrorSucursal.SUCURSAL_NO_EXISTE.excepcion());
        if (!sucursal.isActiva()) {
            throw ErrorSucursal.SUCURSAL_INACTIVA.excepcion();
        }
    }
}