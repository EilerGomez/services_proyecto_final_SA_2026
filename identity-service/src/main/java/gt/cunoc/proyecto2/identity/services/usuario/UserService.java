package gt.cunoc.proyecto2.identity.services.usuario;


import gt.cunoc.proyecto2.identity.dtos.usuario.AdminCreateUserRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.RolesRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;


/**
 * Administración de usuarios y perfiles.
 */
public interface UserService {


    UserResponse ver(UUID usuarioId);


    Page<UserResponse> listar(Pageable paginado);


    UserResponse crear(AdminCreateUserRequest request);


    UserResponse actualizarPerfil(UUID usuarioId, UpdateProfileRequest request);


    UserResponse asignarRoles(UUID usuarioId, RolesRequest request);


    UserResponse cambiarEstado(UUID usuarioId, UserStatusRequest request);

}
