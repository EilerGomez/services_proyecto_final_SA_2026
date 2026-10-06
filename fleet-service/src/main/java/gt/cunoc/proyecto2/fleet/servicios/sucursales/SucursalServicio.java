/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.servicios.sucursales;

/**
 *
 * @author eiler
 */
import gt.cunoc.proyecto2.fleet.dtos.sucursales.ActualizarSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.CambiarEstadoSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.NuevaSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalRespuesta;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import java.util.List;
import java.util.UUID;

public interface SucursalServicio {

    List<SucursalEntidad> listar(Boolean activa);

    SucursalEntidad obtenerPorId(UUID id);

    SucursalRespuesta crear(NuevaSucursalSolicitud solicitud);

    SucursalRespuesta actualizar(UUID id, ActualizarSucursalSolicitud solicitud);

    SucursalRespuesta cambiarEstado(UUID id, CambiarEstadoSucursalSolicitud solicitud);
}
