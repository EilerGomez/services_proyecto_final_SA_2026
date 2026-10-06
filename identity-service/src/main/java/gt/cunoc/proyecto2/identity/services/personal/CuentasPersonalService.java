package gt.cunoc.proyecto2.identity.services.personal;


import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;


/**
 * R5: cuentas de personal y choferes creadas desde Fleet (idempotente por solicitudId).
 */
public interface CuentasPersonalService {

    UserResponse crear(CuentaPersonalRequest solicitud);
}
