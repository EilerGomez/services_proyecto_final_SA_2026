package gt.cunoc.proyecto2.identity.controllers.usuario;

import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdministradorSistemaRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioBusquedaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioDetalleResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioListaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserStatusRequest;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Usuarios: listado con filtros (I11), detalle (I12), alta de admin (I13), estado (I14),
 * búsqueda por correo y promoción a administrador del sistema.
 */
@RestController
@RequestMapping("/identity/usuarios")
public class UserController {

    private final UserService servicio;

    public UserController(UserService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<List<UsuarioListaResponse>> listar(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false, name = "q") String busqueda,
            Pageable paginado) {
        return ResponseEntity.ok(servicio.listar(rol, estado, busqueda, paginado).getContent());
    }

    @PostMapping("/administradores-sistema")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<UsuarioDetalleResponse> crearAdministrador(
            @RequestBody @Valid AdministradorSistemaRequest solicitud) {
        UsuarioDetalleResponse respuesta = servicio.crearAdministradorSistema(solicitud);
        return ResponseEntity.created(URI.create("/identity/usuarios/" + respuesta.usuarioId())).body(respuesta);
    }

    /** Buscar cuenta por correo exacto (para asignarla como personal de sucursal). */
    @GetMapping("/por-correo")
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    public ResponseEntity<UsuarioBusquedaResponse> buscarPorCorreo(
            @RequestParam String correo,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.buscarPorCorreo(correo, usuario.esAdminSistema()));
    }

    /** Hacer administrador del sistema a una cuenta existente (se le avisa por correo). */
    @PostMapping("/{id}/administrador-sistema")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<UsuarioDetalleResponse> hacerAdministradorSistema(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.hacerAdministradorSistema(id));
    }

    @GetMapping("/{id}")
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    public ResponseEntity<UsuarioDetalleResponse> ver(
            @PathVariable UUID id,
            @Parameter(hidden = true) UsuarioActual usuario) {
        UsuarioDetalleResponse respuesta = servicio.detalle(id);
        if (!usuario.esAdminSistema()
                && (respuesta.sucursalId() == null || !respuesta.sucursalId().equals(usuario.sucursalId()))) {
            throw new ProhibidoException("SUCURSAL_AJENA", "No tiene acceso a los datos de esta sucursal");
        }
        return ResponseEntity.ok(respuesta);
    }

    @PatchMapping("/{id}/estado")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<UsuarioDetalleResponse> cambiarEstado(
            @PathVariable UUID id,
            @RequestBody @Valid UserStatusRequest solicitud) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, solicitud));
    }
}
