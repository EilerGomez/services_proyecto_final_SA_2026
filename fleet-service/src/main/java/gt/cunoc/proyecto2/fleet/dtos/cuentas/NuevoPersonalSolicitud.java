/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.dtos.cuentas;

/**
 *
 * @author eiler
 */
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Cuerpo de F7 · POST /fleet/sucursales/{id}/personal. */
public record NuevoPersonalSolicitud(
        @NotNull RolPersonal rol,
        @NotNull LocalDate fechaAsignacion,
        @NotNull @Valid DatosCuentaSolicitud cuenta) {
}
