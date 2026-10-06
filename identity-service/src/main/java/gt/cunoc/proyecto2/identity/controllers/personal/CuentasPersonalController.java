package gt.cunoc.proyecto2.identity.controllers.personal;


import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalRequest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;
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
 * <p>Servicio a servicio (red interna).
 */
@RestController
@RequestMapping("/v1/identity/cuentas-personal")
public class CuentasPersonalController {

    private final CuentasPersonalService servicio;

    public CuentasPersonalController(CuentasPersonalService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<UserResponse> crear(@RequestBody @Valid CuentaPersonalRequest solicitud) {
        UserResponse respuesta = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/v1/identity/usuarios/" + respuesta.id())).body(respuesta);
    }
}
