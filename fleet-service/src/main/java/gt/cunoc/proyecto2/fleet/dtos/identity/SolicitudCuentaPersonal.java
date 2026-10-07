/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.dtos.identity;

/**
 *
 * @author eiler
 */
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import java.util.UUID;

/** Cuerpo de R5 · POST /identity/interno/cuentas-personal (lo que Fleet manda a Identity). */
public record SolicitudCuentaPersonal(
        UUID solicitudId,
        String rol,
        UUID sucursalId,
        UUID choferId,
        String correo,
        String nombres,
        String apellidos,
        String nit,
        String dpi,
        String telefono,
        String direccion,
        String fotoUrl) {

    /** Cuenta de ADMIN_SUCURSAL o CAJERO. solicitudId = id de la fila de personal_sucursal. */
    public static SolicitudCuentaPersonal paraPersonal(UUID personalId, UUID sucursalId, RolPersonal rol,
                                                       DatosCuentaSolicitud datos) {
        return new SolicitudCuentaPersonal(personalId, rol.name(), sucursalId, null,
                datos.correo(), datos.nombres(), datos.apellidos(), datos.nit(), datos.dpi(),
                datos.telefono(), datos.direccion(), datos.fotoUrl());
    }
}
