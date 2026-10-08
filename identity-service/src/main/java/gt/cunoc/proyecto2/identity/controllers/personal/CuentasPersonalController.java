package gt.cunoc.proyecto2.identity.controllers.personal;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalResponse;
import gt.cunoc.proyecto2.identity.services.personal.CuentasPersonalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * R5 de Fleet: crea la cuenta de personal o chofer (idempotente por solicitudId).
 *
 * <p>Solo red interna con token de servicio.
 */
@RestController
@RequestMapping("/identity/interno/cuentas-personal")
@RequiereRol(Roles.SERVICIO)
public class CuentasPersonalController {

    private final CuentasPersonalService servicio;

    public CuentasPersonalController(CuentasPersonalService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<CuentaPersonalResponse> crear(@RequestBody @Valid CuentaPersonalRequest solicitud) {
        CuentaPersonalResponse respuesta = servicio.crear(solicitud);
        if (respuesta.creada()) {
            return ResponseEntity.created(URI.create("/identity/usuarios/" + respuesta.usuarioId())).body(respuesta);
        }
        return ResponseEntity.ok(respuesta);
    }
}
