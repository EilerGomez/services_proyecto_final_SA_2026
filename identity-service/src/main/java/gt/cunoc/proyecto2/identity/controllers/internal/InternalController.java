package gt.cunoc.proyecto2.identity.controllers.internal;


import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


/**
 * R4 de Notification: administradores de una sucursal y datos del chofer.
 *
 * <p>Solo red interna (sin roles de usuario).
 */
@RestController
@RequestMapping("/v1/identity/internal")
public class InternalController {

    private final UsuarioRepository repositorioUsuarios;

    private final UserService servicioUsuarios;

    public InternalController(UsuarioRepository repositorioUsuarios, UserService servicioUsuarios) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.servicioUsuarios = servicioUsuarios;
    }

    @GetMapping("/sucursales/{id}/administradores")
    public ResponseEntity<List<UserResponse>> administradores(@PathVariable UUID id) {
        List<UserResponse> respuesta = repositorioUsuarios
                .findBySucursalIdAndRoles_Codigo(id, Roles.ADMIN_SUCURSAL).stream()
                .map(usuario -> servicioUsuarios.ver(usuario.getId()))
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/choferes/{choferId}")
    public ResponseEntity<UserResponse> chofer(@PathVariable UUID choferId) {
        Usuario usuario = repositorioUsuarios.findByChoferId(choferId).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(servicioUsuarios.ver(usuario.getId()));
    }
}
