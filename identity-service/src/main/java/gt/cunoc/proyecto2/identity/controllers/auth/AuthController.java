package gt.cunoc.proyecto2.identity.controllers.auth;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.RestablecimientoRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest;
import gt.cunoc.proyecto2.identity.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Sesión y recuperación (I2–I7 públicos salvo perfil y cierre; I1 registro).
 */
@RestController
@RequestMapping("/identity/auth")
public class AuthController {

    private final AuthService servicio;

    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<RegistroResponse> registrar(@RequestBody @Valid RegistroRequest solicitud) {
        RegistroResponse respuesta = servicio.registrar(solicitud);
        return ResponseEntity.created(URI.create("/identity/usuarios/" + respuesta.usuarioId())).body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<SesionResponse> iniciarSesion(@RequestBody @Valid InicioSesionRequest solicitud) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(servicio.iniciarSesion(solicitud));
    }

    @PostMapping("/refrescar")
    public ResponseEntity<SesionResponse> renovarSesion(@RequestBody @Valid TokenRefrescoRequest solicitud) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(servicio.renovarSesion(solicitud));
    }

    @PostMapping("/cerrar-sesion")
    public ResponseEntity<Void> cerrarSesion(
            @RequestBody @Valid TokenRefrescoRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        servicio.cerrarSesion(usuario.usuarioId(), solicitud);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recuperar")
    public ResponseEntity<Void> solicitarRecuperacion(@RequestBody @Valid RecuperacionRequest solicitud) {
        servicio.solicitarRecuperacion(solicitud);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Void> restablecerContrasena(@RequestBody @Valid RestablecimientoRequest solicitud) {
        servicio.restablecerContrasena(solicitud);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/contrasena")
    public ResponseEntity<Void> cambiarContrasena(
            @RequestBody @Valid CambioContrasenaRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        servicio.cambiarContrasena(usuario.usuarioId(), solicitud);
        return ResponseEntity.noContent().build();
    }
}
