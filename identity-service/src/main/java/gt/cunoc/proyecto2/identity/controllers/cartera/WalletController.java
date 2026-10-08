package gt.cunoc.proyecto2.identity.controllers.cartera;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletMovementResponse;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletRechargeRequest;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletResponse;
import gt.cunoc.proyecto2.identity.services.cartera.WalletService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * Cartera propia (I17–I19, solo CLIENTE).
 */
@RestController
@RequestMapping("/identity/cartera")
@RequiereRol(Roles.CLIENTE)
public class WalletController {

    private final WalletService servicio;

    public WalletController(WalletService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> saldo(@Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.saldo(usuario.usuarioId()));
    }

    @PostMapping("/recargas")
    public ResponseEntity<WalletMovementResponse> recargar(
            @RequestBody @Valid WalletRechargeRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        WalletMovementResponse respuesta = servicio.recargar(usuario.usuarioId(), solicitud);
        return ResponseEntity.created(URI.create("/identity/cartera/movimientos")).body(respuesta);
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<WalletMovementResponse>> movimientos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Pageable paginado,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.movimientos(usuario.usuarioId(), desde, hasta, paginado).getContent());
    }
}
