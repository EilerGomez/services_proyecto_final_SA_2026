package gt.cunoc.proyecto2.trips.servicios.operaciones;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarLlegadaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarSalidaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.AlquilerPrivadoRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.LlegadaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.SalidaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.tarifas.VistaTarifaDepreciacionRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    public OperacionViajeServicioImpl(ViajeServicio viajes, ViajeRepositorio viajeRepositorio,
                                      SalidaViajeRepositorio salidas, LlegadaViajeRepositorio llegadas,
                                      VistaTarifaDepreciacionRepositorio tarifas,
                                      AlquilerPrivadoRepositorio alquileres) {
        this.viajes = viajes;
        this.viajeRepositorio = viajeRepositorio;
        this.salidas = salidas;
        this.llegadas = llegadas;
        this.tarifas = tarifas;
        this.alquileres = alquileres;
    }

    @Override
    @Transactional
    public ViajeDetalleRespuesta registrarSalida(UUID viajeId, RegistrarSalidaSolicitud solicitud,
                                                 UsuarioActual usuario) {
        ViajeEntidad viaje = viajes.obtenerPorId(viajeId);
        verificarPuedeOperar(viaje, usuario);
        viaje.iniciar();                                                        // 409 si ya salió
        salidas.saveAndFlush(solicitud.crearEntidad(viaje, usuario.usuarioId()));
        viaje = viajeRepositorio.saveAndFlush(viaje);
        // Paso 6: aquí se publica el evento ViajeIniciado (businessDate = día de fechaHoraSalida)
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
        llegadas.saveAndFlush(solicitud.crearEntidad(viaje, salida,
                tarifaVigente(Fechas.diaEnGuatemala(solicitud.fechaHoraLlegada())), usuario.usuarioId()));
        viaje = viajeRepositorio.saveAndFlush(viaje);
        if (viaje.esPrivado()) {
            alquileres.findById(viaje.getAlquilerId()).ifPresent(alquiler -> {
                alquiler.completar();
                alquileres.saveAndFlush(alquiler);
            });
        }
        // Paso 6: aquí se publica el evento ViajeCompletado (businessDate = día de fechaHoraLlegada)
        return viajes.detalle(viaje);
    }

    /** El chofer asignado, o un ADMIN_SUCURSAL de la sucursal del viaje. */
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

    private BigDecimal tarifaVigente(LocalDate fecha) {
        return tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(fecha)
                .orElseThrow(() -> ErrorOperacionViaje.SIN_TARIFA_VIGENTE.excepcion(fecha))
                .getMontoPorKm();
    }
}