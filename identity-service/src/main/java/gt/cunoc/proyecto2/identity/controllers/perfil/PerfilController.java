package gt.cunoc.proyecto2.identity.controllers.perfil;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.usuario.CompletarPerfilRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UpdateProfileRequest;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil propio (I8–I9) y completar el perfil de una cuenta creada con Google.
 */
@RestController
@RequestMapping("/identity/perfil")
public class PerfilController {

    private final UserService servicio;

    public PerfilController(UserService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResponseEntity<PerfilResponse> ver(@Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.perfil(usuario.usuarioId()));
    }

    @PutMapping
    public ResponseEntity<PerfilResponse> actualizar(
            @RequestBody @Valid UpdateProfileRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.actualizarPerfil(usuario.usuarioId(), solicitud));
    }

    /** Cuenta creada con Google: completa DPI, teléfono, dirección y NIT (opcional) antes de usar el portal. */
    @PutMapping("/completar")
    public ResponseEntity<PerfilResponse> completar(
            @RequestBody @Valid CompletarPerfilRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.completarPerfil(usuario.usuarioId(), solicitud));
    }
}
