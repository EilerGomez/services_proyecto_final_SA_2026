package gt.cunoc.proyecto2.sales.servicios.proyecciones;

import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.repositorios.asientos.AsientoViajeRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
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

    private final VistaViajeRepositorio viajes;
    private final AsientoViajeRepositorio asientos;
    private final VistaClienteRepositorio clientes;

    public ProyeccionServicioImpl(VistaViajeRepositorio viajes, AsientoViajeRepositorio asientos,
                                  VistaClienteRepositorio clientes) {
        this.viajes = viajes;
        this.asientos = asientos;
        this.clientes = clientes;
    }

    @Override
    @Transactional
    public void guardarViaje(ViajeEvento viaje, Long version) {
        int filas = viajes.guardar(viaje.viajeId(), viaje.rutaId(), viaje.sucursalId(), viaje.origenNombre(),
                viaje.destinoNombre(), viaje.fechaSalida(), viaje.busId(), viaje.busPlaca(), viaje.capacidad(),
                viaje.precioBoleto(), versionDe(version));
        if (filas == 0) {
            registrarIgnorado("viaje", viaje.viajeId(), version);
            return;
        }
        // Trips no permite bajar la capacidad: solo se agregan los asientos que falten
        int nuevos = asientos.crearAsientos(viaje.viajeId(), viaje.capacidad());
        if (nuevos > 0) {
            log.info("Viaje {}: {} asientos creados (capacidad {})", viaje.viajeId(), nuevos, viaje.capacidad());
        }
    }

    @Override
    @Transactional
    public void iniciarViaje(UUID viajeId, Long version) {
        if (viajes.iniciar(viajeId, versionDe(version)) == 0) {
            registrarIgnorado("viaje", viajeId, version);
        }
    }

    @Override
    @Transactional
    public void eliminarViaje(UUID viajeId, Long version) {
        if (viajes.eliminar(viajeId, versionDe(version)) == 0) {
            registrarIgnorado("viaje", viajeId, version);
        }
    }

    @Override
    @Transactional
    public void guardarCliente(UUID usuarioId, String nit, String nombre, String correo) {
        // Mismas reglas que la venta: sin NIT es "CF", sin nombre "Consumidor final"
        ClienteVenta cliente = new ClienteVenta(usuarioId, nit, nombre, correo);
        clientes.guardar(usuarioId, cliente.nit(), cliente.nombre(), cliente.correo());
    }

    private static long versionDe(Long version) {
        return version == null ? 0L : version;
    }

    private static void registrarIgnorado(String que, UUID id, Long version) {
        log.info("Evento de {} {} con version {} ignorado: ya se aplicó una más nueva o no existe", que, id, version);
    }
}
