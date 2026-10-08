package gt.cunoc.proyecto2.fleet.eventos.publicados.sucursales;

import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import java.math.BigDecimal;
import java.util.UUID;

public record SucursalEvento(
        UUID sucursalId,
        String codigo,
        String nombre,
        String departamento,
        String municipio,
        BigDecimal latitud,
        BigDecimal longitud,
        boolean activa) {

    public static SucursalEvento desde(SucursalEntidad sucursal) {
        return new SucursalEvento(sucursal.getId(), sucursal.getCodigo(), sucursal.getNombre(),
                sucursal.getDepartamento(), sucursal.getMunicipio(), sucursal.getLatitud(),
                sucursal.getLongitud(), sucursal.isActiva());
    }
}