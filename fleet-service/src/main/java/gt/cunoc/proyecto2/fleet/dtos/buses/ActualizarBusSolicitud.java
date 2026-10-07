package gt.cunoc.proyecto2.fleet.dtos.buses;

import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.utilidades.Normalizador;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de F13 · PUT /fleet/buses/{id}.
 * placa, sucursalId, estado y kilometraje no se editan aquí.
 */
public record ActualizarBusSolicitud(
        @NotBlank @Size(max = 50) String marca,
        @NotBlank @Size(max = 50) String modelo,
        @NotNull @Min(1950) @Max(2100) Integer anioFabricacion,
        @NotNull @Min(1) @Max(120) Integer capacidadPasajeros,
        @NotBlank @Size(max = 500) String fotoUrl,
        @NotNull @PositiveOrZero Integer version) {

    public ActualizarBusSolicitud {
        marca = Normalizador.texto(marca);
        modelo = Normalizador.texto(modelo);
        fotoUrl = Normalizador.texto(fotoUrl);
    }

    public void aplicarA(BusEntidad bus) {
        bus.setMarca(marca);
        bus.setModelo(modelo);
        bus.setAnioFabricacion(anioFabricacion);
        bus.setCapacidadPasajeros(capacidadPasajeros);
        bus.setFotoUrl(fotoUrl);
    }
}