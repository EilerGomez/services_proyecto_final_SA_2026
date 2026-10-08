package gt.cunoc.proyecto2.fleet.controladores.bitacora;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.dtos.bitacora.ViajeKilometrajeRespuesta;
import gt.cunoc.proyecto2.fleet.servicios.bitacora.BitacoraKilometrajeServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fleet/buses/{busId}/bitacora")
@Tag(name = "Bitácora de kilometraje", description = "Kilometraje de cada viaje completado por un bus")
public class BitacoraKilometrajeControlador {

    private final BitacoraKilometrajeServicio servicio;

    public BitacoraKilometrajeControlador(BitacoraKilometrajeServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "F23 · Viajes completados por el bus, por fecha de llegada (sin fechas: todos)")
    public ResponseEntity<List<ViajeKilometrajeRespuesta>> listar(
            @PathVariable UUID busId,
            @Parameter(description = "Desde (yyyy-MM-dd), inclusive") @RequestParam(required = false) LocalDate desde,
            @Parameter(description = "Hasta (yyyy-MM-dd), inclusive") @RequestParam(required = false) LocalDate hasta,
            @Parameter(hidden = true) UsuarioActual usuario) {
        List<ViajeKilometrajeRespuesta> viajes = servicio.listar(busId, desde, hasta, usuario).stream()
                .map(ViajeKilometrajeRespuesta::new)
                .toList();
        return ResponseEntity.ok(viajes);
    }
}