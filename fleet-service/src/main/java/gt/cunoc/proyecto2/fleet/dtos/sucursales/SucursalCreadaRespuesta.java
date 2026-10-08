package gt.cunoc.proyecto2.fleet.dtos.sucursales;

import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;

/** Respuesta de F3: la sucursal y su admin inicial. */
public record SucursalCreadaRespuesta(
        SucursalRespuesta sucursal,
        PersonalSucursalRespuesta adminInicial) {
}