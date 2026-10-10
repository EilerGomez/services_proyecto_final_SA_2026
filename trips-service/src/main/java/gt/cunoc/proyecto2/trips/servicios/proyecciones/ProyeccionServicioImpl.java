package gt.cunoc.proyecto2.trips.servicios.proyecciones;

import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.trips.repositorios.buses.VistaBusRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.choferes.VistaChoferRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.sucursales.VistaSucursalRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.tarifas.VistaTarifaDepreciacionRepositorio;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upserts en las vistas (SQL en los repositorios). Si no cambia ninguna fila, el evento era viejo
 * o repetido: se deja en el log y se termina sin error (no hay nada que reintentar).
 */
@Service
public class ProyeccionServicioImpl implements ProyeccionServicio {

    private static final Logger log = LoggerFactory.getLogger(ProyeccionServicioImpl.class);

    private final VistaSucursalRepositorio sucursales;
    private final VistaBusRepositorio buses;
    private final VistaChoferRepositorio choferes;
    private final VistaTarifaDepreciacionRepositorio tarifas;

    public ProyeccionServicioImpl(VistaSucursalRepositorio sucursales, VistaBusRepositorio buses,
                                  VistaChoferRepositorio choferes, VistaTarifaDepreciacionRepositorio tarifas) {
        this.sucursales = sucursales;
        this.buses = buses;
        this.choferes = choferes;
        this.tarifas = tarifas;
    }

    @Override
    @Transactional
    public void guardarSucursal(SucursalEvento sucursal, Long version) {
        int filas = sucursales.guardar(sucursal.sucursalId(), sucursal.nombre(), sucursal.latitud(),
                sucursal.longitud(), sucursal.activa(), versionDe(version));
        registrarSiIgnorado(filas, "sucursal", sucursal.sucursalId(), version);
    }

    @Override
    @Transactional
    public void guardarBus(BusEvento bus, Long version) {
        int filas = buses.guardar(bus.busId(), bus.sucursalId(), bus.placa(), bus.capacidadPasajeros(),
                bus.estado(), versionDe(version));
        registrarSiIgnorado(filas, "bus", bus.busId(), version);
    }

    @Override
    @Transactional
    public void desactivarBus(UUID busId, Long version) {
        registrarSiIgnorado(buses.desactivar(busId, versionDe(version)), "bus", busId, version);
    }

    @Override
    @Transactional
    public void guardarChofer(ChoferEvento chofer, Long version) {
        int filas = choferes.guardar(chofer.choferId(), chofer.sucursalId(), chofer.nombreCompleto(),
                chofer.salarioBaseViaje(), chofer.estado(), versionDe(version));
        registrarSiIgnorado(filas, "chofer", chofer.choferId(), version);
    }

    @Override
    @Transactional
    public void desactivarChofer(UUID choferId, Long version) {
        registrarSiIgnorado(choferes.desactivar(choferId, versionDe(version)), "chofer", choferId, version);
    }

    @Override
    @Transactional
    public void guardarTarifa(TarifaDepreciacionCambiada tarifa) {
        int filas = tarifas.guardar(tarifa.tarifaId(), tarifa.montoPorKm(), tarifa.vigenteDesde());
        registrarSiIgnorado(filas, "tarifa de depreciación", tarifa.tarifaId(), null);
    }

    /** Fleet siempre manda aggregateVersion; si faltara, 0 (solo entra si la fila no existe o también es 0). */
    private static long versionDe(Long version) {
        return version == null ? 0L : version;
    }

    private static void registrarSiIgnorado(int filas, String tipo, UUID id, Long version) {
        if (filas == 0) {
            log.info("Evento ignorado: {} {} ya tiene una versión más nueva o no existe (version del evento: {})",
                    tipo, id, version);
        }
    }
}