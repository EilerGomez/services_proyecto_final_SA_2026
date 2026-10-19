package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalResponse;
import gt.cunoc.proyecto2.identity.dtos.personal.VincularCuentaRequest;

/**
 * R5/R5b: cuentas de personal y choferes desde Fleet (idempotente por solicitudId).
 */
public interface CuentasPersonalService {

    CuentaPersonalResponse crear(CuentaPersonalRequest solicitud);

    /**
     * R5b: valida una cuenta existente para ADMIN_SUCURSAL o CAJERO (el rol llega luego con el evento
     * PersonalSucursalAsignado de Fleet). 409 YA_ES_PERSONAL o SOLICITUD_YA_USADA,
     * 422 USUARIO_NO_EXISTE, CUENTA_INACTIVA, CUENTA_BLOQUEADA, ES_CHOFER o ES_ADMIN_SISTEMA.
     */
    CuentaPersonalResponse vincular(VincularCuentaRequest solicitud);
}
