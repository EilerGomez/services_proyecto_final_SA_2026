package gt.cunoc.proyecto2.identity.controllers.internal;


import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdminContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.ContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.EstadoCuentaResponse;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


/**
 * R4 de Notification: administradores de una sucursal y contacto de un usuario.
 * R4c de Fleet: estado de la cuenta (activa, inactiva o bloqueada) antes de reintegrar personal.
 *
 * <p>Solo red interna con token de servicio.
 */
@RestController
@RequestMapping("/identity/interno")
@RequiereRol(Roles.SERVICIO)
public class InternalController {

    private final UserService servicioUsuarios;

    public InternalController(UserService servicioUsuarios) {
        this.servicioUsuarios = servicioUsuarios;
    }

    @GetMapping("/sucursales/{id}/administradores")
    public ResponseEntity<List<AdminContactoResponse>> administradores(@PathVariable UUID id) {
        return ResponseEntity.ok(servicioUsuarios.administradoresDe(id));
    }

    @GetMapping("/usuarios/{id}/contacto")
    public ResponseEntity<ContactoResponse> contacto(@PathVariable UUID id) {
        return ResponseEntity.ok(servicioUsuarios.contactoDe(id));
    }

    @GetMapping("/usuarios/{id}/estado")
    public ResponseEntity<EstadoCuentaResponse> estadoCuenta(@PathVariable UUID id) {
        return ResponseEntity.ok(servicioUsuarios.estadoCuentaDe(id));
    }
}
