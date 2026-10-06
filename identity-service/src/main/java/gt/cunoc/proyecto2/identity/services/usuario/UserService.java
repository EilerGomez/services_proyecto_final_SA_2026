package gt.cunoc.proyecto2.identity.services.usuario;


import gt.cunoc.proyecto2.identity.dtos.usuario.AdministradorSistemaRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.ContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdminContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioDetalleResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioListaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    PerfilResponse actualizarPerfil(UUID usuarioId, UpdateProfileRequest solicitud);

    UsuarioDetalleResponse cambiarEstado(UUID usuarioId, UserStatusRequest solicitud);

    List<AdminContactoResponse> administradoresDe(UUID sucursalId);

    ContactoResponse contactoDe(UUID usuarioId);
}
