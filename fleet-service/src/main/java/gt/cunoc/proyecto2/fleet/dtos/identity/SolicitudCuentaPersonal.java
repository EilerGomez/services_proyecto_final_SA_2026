package gt.cunoc.proyecto2.fleet.dtos.identity;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import java.util.UUID;

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

    /** Cuenta de CHOFER. solicitudId = choferId = id de la fila de choferes. Foto obligatoria. */
    public static SolicitudCuentaPersonal paraChofer(UUID choferId, UUID sucursalId, DatosCuentaSolicitud datos) {
        return new SolicitudCuentaPersonal(choferId, Roles.CHOFER, sucursalId, choferId,
                datos.correo(), datos.nombres(), datos.apellidos(), datos.nit(), datos.dpi(),
                datos.telefono(), datos.direccion(), datos.fotoUrl());
    }
}