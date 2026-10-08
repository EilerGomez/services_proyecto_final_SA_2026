/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.dtos.sucursales;

/**
 *
 * @author eiler
 */

import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.utilidades.Normalizador;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Cuerpo de F4 · PUT /fleet/sucursales/{id}. El código no se edita.
 * version: la que el usuario vio al abrir el formulario (bloqueo optimista).
 */
public record ActualizarSucursalSolicitud(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 60) String departamento,
        @NotBlank @Size(max = 60) String municipio,
        @NotBlank @Size(max = 255) String direccion,
        @Size(max = 20) String telefono,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitud,
        @NotNull @PositiveOrZero Integer version) {

    public ActualizarSucursalSolicitud {
        nombre = Normalizador.texto(nombre);
        departamento = Normalizador.texto(departamento);
        municipio = Normalizador.texto(municipio);
        direccion = Normalizador.texto(direccion);
        telefono = Normalizador.textoOpcional(telefono);
        latitud = Normalizador.coordenada(latitud);
        longitud = Normalizador.coordenada(longitud);
    }

    public void aplicarA(SucursalEntidad sucursal) {
        sucursal.setNombre(nombre);
        sucursal.setDepartamento(departamento);
        sucursal.setMunicipio(municipio);
        sucursal.setDireccion(direccion);
        sucursal.setTelefono(telefono);
        sucursal.setLatitud(latitud);
        sucursal.setLongitud(longitud);
    }
}
