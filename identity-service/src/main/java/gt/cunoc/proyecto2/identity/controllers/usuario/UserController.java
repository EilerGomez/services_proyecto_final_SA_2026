package gt.cunoc.proyecto2.identity.controllers.usuario;


import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdminCreateUserRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.RolesRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;


/**
 * Administración de usuarios (solo ADMIN_SISTEMA) y perfil propio.
 */
@RestController
@RequestMapping("/v1/identity/usuarios")
public class UserController {

    private final UserService servicio;

    public UserController(UserService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<Page<UserResponse>> listar(Pageable paginado) {
        return ResponseEntity.ok(servicio.listar(paginado));
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<UserResponse> crear(@RequestBody @Valid AdminCreateUserRequest solicitud) {
        UserResponse respuesta = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/v1/identity/usuarios/" + respuesta.id())).body(respuesta);
    }

    @GetMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<UserResponse> ver(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.ver(id));
    }

    @PutMapping("/perfil")
    public ResponseEntity<UserResponse> actualizarPerfil(
            @RequestBody @Valid UpdateProfileRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.actualizarPerfil(usuario.usuarioId(), solicitud));
    }

    @PutMapping("/{id}/roles")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<UserResponse> asignarRoles(
            @PathVariable UUID id,
            @RequestBody @Valid RolesRequest solicitud) {
        return ResponseEntity.ok(servicio.asignarRoles(id, solicitud));
    }

    @PatchMapping("/{id}/estado")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<UserResponse> cambiarEstado(
            @PathVariable UUID id,
            @RequestBody @Valid UserStatusRequest solicitud) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, solicitud));
    }
}
