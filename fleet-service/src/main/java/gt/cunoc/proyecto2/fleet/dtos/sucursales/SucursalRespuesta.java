/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.dtos.sucursales;

/**
 *
 * @author eiler
 */
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import java.math.BigDecimal;
import java.util.UUID;

/** Respuesta de F1, F2, F3, F4 y F5. */
public record SucursalRespuesta(
        UUID sucursalId,
        String codigo,
        String nombre,
        String departamento,
        String municipio,
        String direccion,
        String telefono,
        BigDecimal latitud,
        BigDecimal longitud,
        boolean activa,
        Integer version) {

    public SucursalRespuesta(SucursalEntidad sucursal) {
        this(sucursal.getId(),
                sucursal.getCodigo(),
                sucursal.getNombre(),
                sucursal.getDepartamento(),
                sucursal.getMunicipio(),
                sucursal.getDireccion(),
                sucursal.getTelefono(),
                sucursal.getLatitud(),
                sucursal.getLongitud(),
                sucursal.isActiva(),
                sucursal.getVersion());
    }
}
