/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.dtos.sucursales;

/**
 *
 * @author eiler
 */
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Cuerpo de F5 · PATCH /fleet/sucursales/{id}/estado. */
public record CambiarEstadoSucursalSolicitud(
        @NotNull Boolean activa,
        @NotNull @PositiveOrZero Integer version) {
}
