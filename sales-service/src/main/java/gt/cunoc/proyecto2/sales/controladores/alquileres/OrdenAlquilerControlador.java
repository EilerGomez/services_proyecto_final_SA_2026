package gt.cunoc.proyecto2.sales.controladores.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.dtos.alquileres.OrdenAlquilerRespuesta;
import gt.cunoc.proyecto2.sales.dtos.alquileres.PagoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.servicios.alquileres.OrdenAlquilerServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales/ordenes-alquiler")
@Tag(name = "Órdenes de alquiler", description = "Pago de alquileres privados con cartera o en efectivo")
public class OrdenAlquilerControlador {

    private final OrdenAlquilerServicio servicio;

    public OrdenAlquilerControlador(OrdenAlquilerServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Roles.CAJERO, Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
    @Operation(summary = "S9 · Órdenes de la sucursal (filtro por estado)")
    public ResponseEntity<List<OrdenAlquilerRespuesta>> listar(
            @Parameter(description = "Solo ADMIN_SISTEMA puede pedir otra sucursal")
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) EstadoOrdenAlquiler estado,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.listar(new FiltroOrdenes(sucursalId, estado).paraUsuario(usuario)));
    }

    @GetMapping("/mias")
    @RequiereRol(Roles.CLIENTE)
    @Operation(summary = "S10 · Mis órdenes de pago de alquiler")
    public ResponseEntity<List<OrdenAlquilerRespuesta>> mias(@Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.mias(usuario));
    }

    @GetMapping("/{id}")
    @RequiereRol({Roles.CLIENTE, Roles.CAJERO, Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
    @Operation(summary = "S11 · Detalle de una orden (el frontend la consulta mientras se paga con cartera)")
    public ResponseEntity<OrdenAlquilerRespuesta> obtener(@PathVariable UUID id,
                                                         @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.obtener(id, usuario));
    }

    @PostMapping("/{id}/pago-cartera")
    @RequiereRol(Roles.CLIENTE)
    @Operation(summary = "S12 · Pagar con la cartera (consultar S11 hasta que quede PAGADA o RECHAZADA)")
    public ResponseEntity<OrdenAlquilerRespuesta> pagarConCartera(@PathVariable UUID id,
                                                                  @RequestBody @Valid PagoOrdenSolicitud solicitud,
                                                                  @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(servicio.pagarConCartera(id, solicitud, usuario));
    }

    @PostMapping("/{id}/pago-efectivo")
    @RequiereRol({Roles.CAJERO, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "S13 · Pagar en efectivo en mostrador")
    public ResponseEntity<OrdenAlquilerRespuesta> pagarEnEfectivo(@PathVariable UUID id,
                                                                  @RequestBody @Valid PagoOrdenSolicitud solicitud,
                                                                  @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.pagarEnEfectivo(id, solicitud, usuario));
    }
}