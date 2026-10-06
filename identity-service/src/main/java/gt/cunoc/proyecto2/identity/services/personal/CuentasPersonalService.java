package gt.cunoc.proyecto2.identity.services.personal;


import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalResponse;


/**
 * R5: cuentas de personal y choferes creadas desde Fleet (idempotente por solicitudId).
 */
public interface CuentasPersonalService {

    CuentaPersonalResponse crear(CuentaPersonalRequest solicitud);
}
