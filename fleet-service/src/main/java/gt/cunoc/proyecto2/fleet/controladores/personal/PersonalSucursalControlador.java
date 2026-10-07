package gt.cunoc.proyecto2.fleet.controladores.personal;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.dtos.personal.DesactivarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.NuevoPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladoPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.servicios.personal.PersonalSucursalServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fleet")
@Tag(name = "Personal de sucursal", description = "Administradores de sucursal y cajeros")
public class PersonalSucursalControlador {

    private final PersonalSucursalServicio servicio;

    public PersonalSucursalControlador(PersonalSucursalServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/sucursales/{sucursalId}/personal")
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL, Roles.CLIENTE})
    @Operation(summary = "F6 · Lista el personal de una sucursal (ADMIN_SUCURSAL: solo la suya)")
    public ResponseEntity<List<PersonalSucursalRespuesta>> listar(
            @PathVariable UUID sucursalId,
            @RequestParam(required = false) RolPersonal rol,
            @RequestParam(required = false) Boolean activo,
            @Parameter(hidden = true) UsuarioActual usuario) {
        List<PersonalSucursalRespuesta> personal = servicio.listar(sucursalId, rol, activo, usuario).stream()
                .map(PersonalSucursalRespuesta::new)
                .toList();
        return ResponseEntity.ok(personal);
    }

    @PostMapping("/sucursales/{sucursalId}/personal")
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "F7 · Agrega un admin o cajero y crea su cuenta (ADMIN_SUCURSAL: solo cajeros)")
    public ResponseEntity<PersonalSucursalRespuesta> agregar(
            @PathVariable UUID sucursalId,
            @RequestBody @Valid NuevoPersonalSolicitud solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.agregar(sucursalId, solicitud, usuario));
    }

    @PatchMapping("/personal/{id}/desactivar")
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "F8 · Desactiva a una persona (ADMIN_SUCURSAL: solo cajeros de su sucursal)")
    public ResponseEntity<PersonalSucursalRespuesta> desactivar(
            @PathVariable UUID id,
            @RequestBody @Valid DesactivarPersonalSolicitud solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.desactivar(id, solicitud, usuario));
    }

    @PostMapping("/personal/{id}/traslado")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    @Operation(summary = "F9 · Traslada a una persona a otra sucursal")
    public ResponseEntity<TrasladoPersonalRespuesta> trasladar(
            @PathVariable UUID id,
            @RequestBody @Valid TrasladarPersonalSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.trasladar(id, solicitud));
    }
}