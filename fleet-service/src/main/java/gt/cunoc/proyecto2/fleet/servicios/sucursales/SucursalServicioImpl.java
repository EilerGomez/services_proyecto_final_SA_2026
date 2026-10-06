/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.servicios.sucursales;

import gt.cunoc.proyecto2.fleet.dtos.sucursales.ActualizarSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.CambiarEstadoSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.NuevaSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalRespuesta;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.utilidades.VerificadorVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author eiler
 */

@Service
public class SucursalServicioImpl implements SucursalServicio {

    private final SucursalRepositorio repositorio;

    public SucursalServicioImpl(SucursalRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SucursalEntidad> listar(Boolean activa) {
        if (activa == null) {
            return repositorio.findAllByOrderByNombreAsc();
        }
        return repositorio.findByActivaOrderByNombreAsc(activa);
    }

    @Override
    @Transactional(readOnly = true)
    public SucursalEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorSucursal.SUCURSAL_NO_EXISTE.excepcion());
    }

    @Override
    @Transactional
    public SucursalRespuesta crear(NuevaSucursalSolicitud solicitud) {
        if (repositorio.existsByCodigo(solicitud.codigo())) {
            throw ErrorSucursal.SUCURSAL_DUPLICADA.excepcion("código", solicitud.codigo());
        }
        if (repositorio.existsByNombre(solicitud.nombre())) {
            throw ErrorSucursal.SUCURSAL_DUPLICADA.excepcion("nombre", solicitud.nombre());
        }

        // Paso personal_sucursal: aquí se crea el admin inicial con R5
        // (POST /identity/interno/cuentas-personal) antes de guardar.

        // saveAndFlush: la version (0) ya queda asignada para el evento
        SucursalEntidad sucursal = repositorio.saveAndFlush(solicitud.crearEntidad());

        // Aquí se publica el evento SucursalCreada (aggregateVersion = sucursal.getVersion()).
        // Paso personal_sucursal: aquí se publica PersonalSucursalAsignado del admin inicial.

        return new SucursalRespuesta(sucursal);
    }

    @Override
    @Transactional
    public SucursalRespuesta actualizar(UUID id, ActualizarSucursalSolicitud solicitud) {
        SucursalEntidad sucursal = obtenerPorId(id);
        VerificadorVersion.verificar(sucursal.getVersion(), solicitud.version());

        if (repositorio.existsByNombreAndIdNot(solicitud.nombre(), id)) {
            throw ErrorSucursal.SUCURSAL_DUPLICADA.excepcion("nombre", solicitud.nombre());
        }

        solicitud.aplicarA(sucursal);
        sucursal = repositorio.saveAndFlush(sucursal);

        // Aquí se publica el evento SucursalActualizada (aggregateVersion = sucursal.getVersion()).

        return new SucursalRespuesta(sucursal);
    }

    @Override
    @Transactional
    public SucursalRespuesta cambiarEstado(UUID id, CambiarEstadoSucursalSolicitud solicitud) {
        SucursalEntidad sucursal = obtenerPorId(id);
        VerificadorVersion.verificar(sucursal.getVersion(), solicitud.version());

        if (sucursal.isActiva() == solicitud.activa()) {
            return new SucursalRespuesta(sucursal);
        }

        if (!solicitud.activa()) {
            // Paso buses: si la sucursal tiene buses en estado distinto de INACTIVO
            // -> 422 SUCURSAL_CON_BUSES_ACTIVOS.
        }

        sucursal.setActiva(solicitud.activa());
        sucursal = repositorio.saveAndFlush(sucursal);

        // Aquí se publica el evento SucursalActualizada (aggregateVersion = sucursal.getVersion()).

        return new SucursalRespuesta(sucursal);
    }
}
