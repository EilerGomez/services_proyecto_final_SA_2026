package gt.cunoc.proyecto2.wallet.controllers;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.wallet.dtos.WalletMovementResponse;
import gt.cunoc.proyecto2.wallet.dtos.WalletRechargeRequest;
import gt.cunoc.proyecto2.wallet.dtos.WalletResponse;
import gt.cunoc.proyecto2.wallet.services.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Cartera propia (W1–W3, solo CLIENTE). Antes: I17–I19 en /identity/cartera.
 */
@RestController
@RequestMapping("/wallet/cartera")
@RequiereRol(Roles.CLIENTE)
@Tag(name = "Cartera", description = "Saldo, recargas e historial de la cartera digital del cliente")
public class WalletController {

    private final WalletService servicio;

    public WalletController(WalletService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "W1 · Saldo de mi cartera")
    public ResponseEntity<WalletResponse> saldo(@Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.saldo(usuario.usuarioId()));
    }

    @PostMapping("/recargas")
    @Operation(summary = "W2 · Recargar (cobra la pasarela de pagos configurada)")
    public ResponseEntity<WalletMovementResponse> recargar(
            @RequestBody @Valid WalletRechargeRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        WalletMovementResponse respuesta = servicio.recargar(usuario.usuarioId(), solicitud);
        return ResponseEntity.created(URI.create("/wallet/cartera/movimientos")).body(respuesta);
    }

    @GetMapping("/movimientos")
    @Operation(summary = "W3 · Historial de movimientos (filtro opcional por fechas)")
    public ResponseEntity<List<WalletMovementResponse>> movimientos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Pageable paginado,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.movimientos(usuario.usuarioId(), desde, hasta, paginado).getContent());
    }
}
