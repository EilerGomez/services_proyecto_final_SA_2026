package gt.cunoc.proyecto2.fleet.dtos.sucursales;

import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.utilidades.Normalizador;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cuerpo de F3 · POST /fleet/sucursales: la sucursal y su admin inicial.
 * fechaAsignacion: fecha (elegida por el usuario) desde la que el admin inicial trabaja ahí.
 */
public record NuevaSucursalSolicitud(
        @NotBlank @Size(max = 10) @Pattern(regexp = "^[A-Z0-9_-]+$",
                message = "solo letras, números, guion y guion bajo") String codigo,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 60) String departamento,
        @NotBlank @Size(max = 60) String municipio,
        @NotBlank @Size(max = 255) String direccion,
        @Size(max = 20) String telefono,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud,
        @NotNull LocalDate fechaAsignacion,
        @NotNull @Valid DatosCuentaSolicitud adminInicial) {

    /** Se ejecuta al leer el JSON, antes de las validaciones. */
    public NuevaSucursalSolicitud {
        codigo = Normalizador.codigo(codigo);
        nombre = Normalizador.texto(nombre);
        departamento = Normalizador.texto(departamento);
        municipio = Normalizador.texto(municipio);
        direccion = Normalizador.texto(direccion);
        telefono = Normalizador.textoOpcional(telefono);
        latitud = Normalizador.coordenada(latitud);
        longitud = Normalizador.coordenada(longitud);
    }

    public SucursalEntidad crearEntidad() {
        SucursalEntidad sucursal = new SucursalEntidad();
        sucursal.setId(UUID.randomUUID());
        sucursal.setCodigo(codigo);
        sucursal.setNombre(nombre);
        sucursal.setDepartamento(departamento);
        sucursal.setMunicipio(municipio);
        sucursal.setDireccion(direccion);
        sucursal.setTelefono(telefono);
        sucursal.setLatitud(latitud);
        sucursal.setLongitud(longitud);
        sucursal.setActiva(true);
        return sucursal;
    }
}