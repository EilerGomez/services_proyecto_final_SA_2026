package gt.cunoc.proyecto2.wallet.controllers;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.wallet.dtos.CarteraSistemaResponse;
import gt.cunoc.proyecto2.wallet.dtos.WalletMovementResponse;
import gt.cunoc.proyecto2.wallet.services.CarteraSistemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Billetera del sistema (W4–W5, solo ADMIN_SISTEMA). Solo consulta: no hay retiros.
 */
@RestController
@RequestMapping("/wallet/sistema")
@RequiereRol(Roles.ADMIN_SISTEMA)
@Tag(name = "Billetera del sistema", description = "Dinero de las compras en línea y de los reembolsos")
public class CarteraSistemaController {

    private final CarteraSistemaService servicio;

    public CarteraSistemaController(CarteraSistemaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "W4 · Saldo y totales de la billetera del sistema")
    public ResponseEntity<CarteraSistemaResponse> saldo() {
        return ResponseEntity.ok(servicio.saldo());
    }

    @GetMapping("/movimientos")
    @Operation(summary = "W5 · Cobros y devoluciones (filtros opcionales: tipo, desde, hasta)")
    public ResponseEntity<List<WalletMovementResponse>> movimientos(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Pageable paginado) {
        return ResponseEntity.ok(servicio.movimientos(tipo, desde, hasta, paginado).getContent());
    }
}
