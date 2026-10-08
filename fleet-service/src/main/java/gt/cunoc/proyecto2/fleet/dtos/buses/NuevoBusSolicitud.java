package gt.cunoc.proyecto2.fleet.dtos.buses;

import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.utilidades.Normalizador;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cuerpo de F12 · POST /fleet/buses.
 * fotoUrl: la URL que devolvió F22 (obligatoria). fechaRegistro: fecha de negocio del evento BusCreado.
 */
public record NuevoBusSolicitud(
        @NotNull UUID sucursalId,
        @NotBlank @Pattern(regexp = "^[A-Z0-9-]{5,10}$", message = "de 5 a 10 letras, números o guion") String placa,
        @NotBlank @Size(max = 50) String marca,
        @NotBlank @Size(max = 50) String modelo,
        @NotNull @Min(1950) @Max(2100) Integer anioFabricacion,
        @NotNull @Min(1) @Max(120) Integer capacidadPasajeros,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal kilometrajeActual,
        @NotBlank @Size(max = 500) String fotoUrl,
        @NotNull LocalDate fechaRegistro) {

    public NuevoBusSolicitud {
        placa = Normalizador.codigo(placa);
        marca = Normalizador.texto(marca);
        modelo = Normalizador.texto(modelo);
        fotoUrl = Normalizador.texto(fotoUrl);
        kilometrajeActual = kilometrajeActual == null ? BigDecimal.ZERO : kilometrajeActual;
    }

    public BusEntidad crearEntidad() {
        return BusEntidad.registrar(sucursalId, placa, marca, modelo, anioFabricacion,
                capacidadPasajeros, kilometrajeActual, fotoUrl);
    }
}