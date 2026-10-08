package gt.cunoc.proyecto2.identity.dtos.auth;


/**
 * Resultado del login: sesión completa o desafío de doble factor (202).
 */
public sealed interface ResultadoInicioSesion permits SesionResponse, DesafioDobleFactor {
}
