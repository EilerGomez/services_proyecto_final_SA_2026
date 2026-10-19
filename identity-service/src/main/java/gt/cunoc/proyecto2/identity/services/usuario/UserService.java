package gt.cunoc.proyecto2.identity.services.usuario;


import gt.cunoc.proyecto2.identity.dtos.usuario.AdministradorSistemaRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.ContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.EstadoCuentaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdminContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioBusquedaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioDetalleResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioListaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;


/**
 * Perfiles, usuarios y estados (I8, I9, I11–I14).
 */
public interface UserService {

    PerfilResponse perfil(UUID usuarioId);

    UsuarioDetalleResponse detalle(UUID usuarioId);

    Page<UsuarioListaResponse> listar(String rol, String estado, String q, Pageable paginado);

    UsuarioDetalleResponse crearAdministradorSistema(AdministradorSistemaRequest solicitud);

    /**
     * Busca una cuenta por correo exacto para asignarla como personal de sucursal.
     * motivoDetallado = false (admin de sucursal): no revela si es admin del sistema o chofer.
     * 400 CORREO_REQUERIDO, 404 USUARIO_NO_EXISTE.
     */
    UsuarioBusquedaResponse buscarPorCorreo(String correo, boolean motivoDetallado);

    /**
     * Da el rol ADMIN_SISTEMA a una cuenta existente y le avisa por correo (RolAsignado).
     * 404 USUARIO_NO_EXISTE, 409 YA_ES_ADMIN_SISTEMA,
     * 422 CUENTA_INACTIVA, CUENTA_BLOQUEADA, ES_CHOFER o ES_PERSONAL_SUCURSAL.
     */
    UsuarioDetalleResponse hacerAdministradorSistema(UUID usuarioId);

    PerfilResponse actualizarPerfil(UUID usuarioId, UpdateProfileRequest solicitud);

    UsuarioDetalleResponse cambiarEstado(UUID usuarioId, UserStatusRequest solicitud);

    List<AdminContactoResponse> administradoresDe(UUID sucursalId);

    ContactoResponse contactoDe(UUID usuarioId);

    /** Estado de la cuenta (R4c): lo usa Fleet antes de reintegrar a una persona del personal. */
    EstadoCuentaResponse estadoCuentaDe(UUID usuarioId);
}
