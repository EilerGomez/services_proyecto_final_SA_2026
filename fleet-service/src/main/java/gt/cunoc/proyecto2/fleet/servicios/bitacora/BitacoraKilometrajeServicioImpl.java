package gt.cunoc.proyecto2.fleet.servicios.bitacora;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.errores.bitacora.ErrorBitacora;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.bitacora.BitacoraKilometrajeEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.bitacora.BitacoraKilometrajeRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.buses.BusRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.buses.BusServicio;
import gt.cunoc.proyecto2.fleet.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BitacoraKilometrajeServicioImpl implements BitacoraKilometrajeServicio {
    
    private static final String VIAJE_INICIADO ="ViajeIniciado";
    private static final String VIAJE_COMPLETADO="ViajeCompletado";

    private static final Logger log = LoggerFactory.getLogger(BitacoraKilometrajeServicioImpl.class);

    private final BitacoraKilometrajeRepositorio repositorio;
    private final BusServicio buses;
    private final BusRepositorio busRepositorio;
    private final PublicadorEventosFleet eventos;

    public BitacoraKilometrajeServicioImpl(BitacoraKilometrajeRepositorio repositorio, BusServicio buses,
                                           BusRepositorio busRepositorio, PublicadorEventosFleet eventos) {
        this.repositorio = repositorio;
        this.buses = buses;
        this.busRepositorio = busRepositorio;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BitacoraKilometrajeEntidad> ultimosViajes(UUID busId) {
        return repositorio.findTop5ByBusIdOrderByFechaLlegadaDesc(busId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BitacoraKilometrajeEntidad> listar(UUID busId, LocalDate desde, LocalDate hasta,
                                                   UsuarioActual usuario) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw ErrorBitacora.RANGO_FECHAS_INVALIDO.excepcion(desde, hasta);
        }
        buses.obtener(busId, usuario);                      // 404 si no existe, 403 si es de otra sucursal
        return repositorio.buscarPorRango(busId, Fechas.inicioDelDia(desde), Fechas.finDelDia(hasta));
    }

    @Override
    @Transactional
    public boolean registrarViajeIniciado(UUID busId, UUID viajeId, LocalDate fechaNegocio) {
        if (repositorio.existsByViajeId(viajeId)) {
            return false;                                   // el viaje ya terminó: un ViajeIniciado tardío no lo pone EN_RUTA
        }
        
        Optional<BusEntidad> encontrado = buscarBus(busId, VIAJE_INICIADO);
        
        if (encontrado.isEmpty() || !encontrado.get().registrarSalida()) {
            return false;
        }
        
        BusEntidad bus = busRepositorio.saveAndFlush(encontrado.get());
        eventos.busActualizado(bus, fechaNegocio);
        return true;
    }

    @Override
    @Transactional
    public boolean registrarViajeCompletado(UUID busId, UUID viajeId, BigDecimal kmInicial, BigDecimal kmFinal,
                                            OffsetDateTime fechaLlegada, LocalDate fechaNegocio) {
        if (repositorio.existsByViajeId(viajeId)) {
            return false;                                   // evento repetido: el viaje ya sumó kilometraje
        }
        
        Optional<BusEntidad> encontrado = buscarBus(busId, VIAJE_COMPLETADO);
        if (encontrado.isEmpty()) {
            return false;
        }

        repositorio.save(BitacoraKilometrajeEntidad.registrar(busId, viajeId, kmInicial, kmFinal, fechaLlegada));
        BusEntidad bus = encontrado.get();
        bus.registrarLlegada(kmFinal);
        bus = busRepositorio.saveAndFlush(bus);
        eventos.busActualizado(bus, fechaNegocio);          // Reporting y Trips reciben el kilometraje nuevo
        return true;
    }

    /** Un evento de un bus que no existe no se reintenta (nunca va a existir): se registra y se ignora. */
    private Optional<BusEntidad> buscarBus(UUID busId, String evento) {
        Optional<BusEntidad> bus = busRepositorio.findById(busId);
        if (bus.isEmpty()) {
            log.warn("{} de un bus que no existe en Fleet: {}", evento, busId);
        }
        return bus;
    }
}