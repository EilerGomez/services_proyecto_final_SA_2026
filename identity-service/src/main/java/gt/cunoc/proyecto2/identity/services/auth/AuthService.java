package gt.cunoc.proyecto2.identity.services.auth;


import gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.DobleFactorRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.RestablecimientoRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.ResultadoInicioSesion;
import gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.VerificarDobleFactorRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;

import java.util.UUID;


/**
 * Autenticación y cuentas propias: sesión, registro, renovación y recuperación.
 */
public interface AuthService {

    RegistroResponse registrar(RegistroRequest solicitud);

    ResultadoInicioSesion iniciarSesion(InicioSesionRequest solicitud);

    SesionResponse verificarDobleFactor(VerificarDobleFactorRequest solicitud);

    void cambiarDobleFactor(UUID usuarioId, DobleFactorRequest solicitud);

    SesionResponse renovarSesion(TokenRefrescoRequest solicitud);

    void cerrarSesion(UUID usuarioId, TokenRefrescoRequest solicitud);

    void solicitarRecuperacion(RecuperacionRequest solicitud);

    void restablecerContrasena(RestablecimientoRequest solicitud);

    PerfilResponse miPerfil(UUID usuarioId);

    void cambiarContrasena(UUID usuarioId, CambioContrasenaRequest solicitud);
}
