package gt.cunoc.proyecto2.fleet.servicios.bitacora;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.errores.bitacora.ErrorBitacora;
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
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BitacoraKilometrajeServicioImpl implements BitacoraKilometrajeServicio {

    private final BitacoraKilometrajeRepositorio repositorio;
    private final BusServicio buses;
    private final BusRepositorio busRepositorio;

    public BitacoraKilometrajeServicioImpl(BitacoraKilometrajeRepositorio repositorio, BusServicio buses,
                                           BusRepositorio busRepositorio) {
        this.repositorio = repositorio;
        this.buses = buses;
        this.busRepositorio = busRepositorio;
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
        buses.obtener(busId, usuario);                     
        return repositorio.buscarPorRango(busId, Fechas.inicioDelDia(desde), Fechas.finDelDia(hasta));
    }

    @Override
    @Transactional
    public boolean registrarViajeCompletado(UUID busId, UUID viajeId, BigDecimal kmInicial, BigDecimal kmFinal,
                                            OffsetDateTime fechaLlegada) {
        if (repositorio.existsByViajeId(viajeId)) {
            return false;                                   
        }
        BusEntidad bus = buses.obtenerPorId(busId);

        repositorio.save(BitacoraKilometrajeEntidad.registrar(busId, viajeId, kmInicial, kmFinal, fechaLlegada));
        bus.registrarLlegada(kmFinal);
        bus = busRepositorio.saveAndFlush(bus);

        // Aquí se publica el evento BusActualizado con el kilometraje nuevo
        // (aggregateVersion = bus.getVersion(), businessDate = Fechas.diaEnGuatemala(fechaLlegada)).

        return true;
    }
}