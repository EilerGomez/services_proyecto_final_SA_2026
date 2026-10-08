package gt.cunoc.proyecto2.identity.controllers.tarifa;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.dtos.tarifa.CreateDepreciationRateRequest;
import gt.cunoc.proyecto2.identity.dtos.tarifa.DepreciationRateResponse;
import gt.cunoc.proyecto2.identity.services.tarifa.TarifaService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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

/**
 * Tarifas de depreciación (I15, I16).
 */
@RestController
@RequestMapping("/identity/tarifas-depreciacion")
public class DepreciationRateController {

    private final TarifaService servicio;

    public DepreciationRateController(TarifaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    public ResponseEntity<?> listar(
            @RequestParam(required = false, name = "vigenteEn")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vigenteEn,
            Pageable paginado,
            @Parameter(hidden = true) UsuarioActual usuario) {
        if (vigenteEn != null) {
            return ResponseEntity.ok(servicio.vigente(vigenteEn));
        }
        Page<DepreciationRateResponse> pagina = servicio.listar(paginado);
        return ResponseEntity.ok(pagina.getContent());
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SISTEMA)
    public ResponseEntity<DepreciationRateResponse> crear(
            @RequestBody @Valid CreateDepreciationRateRequest solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        DepreciationRateResponse respuesta = servicio.crear(solicitud, usuario.usuarioId());
        return ResponseEntity.created(URI.create("/identity/tarifas-depreciacion/" + respuesta.tarifaId())).body(respuesta);
    }
}
