package gt.cunoc.proyecto2.trips.controladores.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.alquileres.ConfiguracionAlquilerRespuesta;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevaConfiguracionAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.servicios.alquileres.ConfiguracionAlquilerServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips/configuracion-alquiler")
@Tag(name = "Configuración de alquiler", description = "Precio por km con el que se cotizan los alquileres privados")
public class ConfiguracionAlquilerControlador {

    private final ConfiguracionAlquilerServicio servicio;

    public ConfiguracionAlquilerControlador(ConfiguracionAlquilerServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "T1 · Historial del precio por km (el más reciente primero)")
    public ResponseEntity<List<ConfiguracionAlquilerRespuesta>> listar() {
        List<ConfiguracionAlquilerRespuesta> precios = servicio.listar().stream()
                .map(ConfiguracionAlquilerRespuesta::new)
                .toList();
        return ResponseEntity.ok(precios);
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SISTEMA)
    @Operation(summary = "T2 · Registra un precio por km desde una fecha")
    public ResponseEntity<ConfiguracionAlquilerRespuesta> registrar(
            @RequestBody @Valid NuevaConfiguracionAlquilerSolicitud solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ConfiguracionAlquilerRespuesta(servicio.registrar(solicitud, usuario)));
    }
}