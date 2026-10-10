package gt.cunoc.proyecto2.sales.controladores.asientos;

import gt.cunoc.proyecto2.sales.dtos.asientos.MapaAsientosRespuesta;
import gt.cunoc.proyecto2.sales.servicios.asientos.AsientoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales/viajes")
@Tag(name = "Asientos", description = "Mapa de asientos de los viajes a la venta")
public class AsientoControlador {

    private final AsientoServicio servicio;

    public AsientoControlador(AsientoServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{viajeId}/asientos")
    @Operation(summary = "S1 · Mapa de asientos de un viaje (público)")
    public ResponseEntity<MapaAsientosRespuesta> mapa(@PathVariable UUID viajeId) {
        return ResponseEntity.ok(servicio.mapa(viajeId));
    }
}