package gt.cunoc.proyecto2.identity.controllers.auth;


import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.auth.CambioContrasenaRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.InicioSesionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RecuperacionRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.RestablecimientoRequest;
import gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.TokenRefrescoRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;
import gt.cunoc.proyecto2.identity.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;


/**
 * Sesión y cuentas propias (público salvo perfil y cierre de sesión).
 */
@RestController
@RequestMapping("/v1/identity/auth")
public class AuthController {

    private final AuthService servicio;

    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<UserResponse> registrar(@RequestBody @Valid RegistroRequest solicitud) {
        UserResponse respuesta = servicio.registrar(solicitud);
        return ResponseEntity.created(URI.create("/v1/identity/usuarios/" + respuesta.id())).body(respuesta);
    }

    @PostMapping("/sesion")
    public ResponseEntity<SesionResponse> iniciarSesion(@RequestBody @Valid InicioSesionRequest solicitud) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(servicio.iniciarSesion(solicitud));
    }

    @PostMapping("/sesion/renovacion")
    public ResponseEntity<SesionResponse> renovarSesion(@RequestBody @Valid TokenRefrescoRequest solicitud) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(servicio.renovarSesion(solicitud));
    }

    @DeleteMapping("/sesion")
    public ResponseEntity<Void> cerrarSesion(
            @RequestBody @Valid TokenRefrescoRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        servicio.cerrarSesion(usuario.usuarioId(), solicitud);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/contrasena/recuperacion")
    public ResponseEntity<Void> solicitarRecuperacion(@RequestBody @Valid RecuperacionRequest solicitud) {
        servicio.solicitarRecuperacion(solicitud);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/contrasena/restablecimiento")
    public ResponseEntity<Void> restablecerContrasena(@RequestBody @Valid RestablecimientoRequest solicitud) {
        servicio.restablecerContrasena(solicitud);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/yo")
    public ResponseEntity<UserResponse> miPerfil(@Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.miPerfil(usuario.usuarioId()));
    }

    @PostMapping("/contrasena/cambio")
    public ResponseEntity<Void> cambiarContrasena(
            @RequestBody @Valid CambioContrasenaRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        servicio.cambiarContrasena(usuario.usuarioId(), solicitud);
        return ResponseEntity.noContent().build();
    }
}
