package gt.cunoc.proyecto2.identity.services.auth;


import gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RestablecimientoRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;

import java.util.UUID;


/**
 * Autenticación y cuentas propias: sesión, registro, renovación y recuperación.
 */
public interface AuthService {

    SesionResponse iniciarSesion(InicioSesionRequest solicitud);

    UserResponse registrar(RegistroRequest solicitud);

    SesionResponse renovarSesion(TokenRefrescoRequest solicitud);

    void cerrarSesion(UUID usuarioId, TokenRefrescoRequest solicitud);

    void solicitarRecuperacion(RecuperacionRequest solicitud);

    void restablecerContrasena(RestablecimientoRequest solicitud);

    UserResponse miPerfil(UUID usuarioId);

    void cambiarContrasena(UUID usuarioId, CambioContrasenaRequest solicitud);
}
