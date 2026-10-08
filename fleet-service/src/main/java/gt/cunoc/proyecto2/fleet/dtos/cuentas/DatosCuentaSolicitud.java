/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.dtos.cuentas;

/**
 *
 * @author eiler
 */
import gt.cunoc.proyecto2.fleet.utilidades.Normalizador;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Datos de la cuenta que se crea en Identity con R5 (admin inicial en F3, personal en F7 y,
 * más adelante, chofer en F18). Los límites son los de la tabla perfiles de Identity.
 *
 * solicitudId (opcional): el frontend genera un UUID al abrir el formulario y lo reenvía si
 * reintenta; así un reintento no crea una segunda cuenta.
 */
public record DatosCuentaSolicitud(
        UUID solicitudId,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Size(max = 20) String nit,
        @NotBlank @Pattern(regexp = "^[0-9]{13}$", message = "debe tener 13 dígitos") String dpi,
        @NotBlank @Size(max = 20) String telefono,
        @NotBlank @Size(max = 255) String direccion,
        @Size(max = 500) String fotoUrl) {

    public DatosCuentaSolicitud {
        correo = Normalizador.correo(correo);
        nombres = Normalizador.texto(nombres);
        apellidos = Normalizador.texto(apellidos);
        nit = Normalizador.codigo(nit);
        dpi = Normalizador.texto(dpi);
        telefono = Normalizador.texto(telefono);
        direccion = Normalizador.texto(direccion);
        fotoUrl = Normalizador.textoOpcional(fotoUrl);
    }
}
