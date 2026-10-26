package gt.cunoc.proyecto2.sales.dtos.ventas;

import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.utilidades.Normalizador;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record NuevaVentaMostradorSolicitud(
        @NotNull UUID viajeId,
        @NotEmpty @Size(max = 60) List<@NotNull @Positive Integer> asientos,
        @Size(max = 20) String clienteNit,
        @Size(max = 201) String clienteNombre,
        @Email @Size(max = 150) String clienteCorreo,
        @NotNull LocalDate fechaPago) {

    public NuevaVentaMostradorSolicitud {
        clienteNit = Normalizador.codigo(Normalizador.textoOpcional(clienteNit));
        clienteNombre = Normalizador.textoOpcional(clienteNombre);
        clienteCorreo = Normalizador.correo(Normalizador.textoOpcional(clienteCorreo));
    }

    public List<Integer> asientosValidados(int capacidad) {
        return Asientos.validar(asientos, capacidad);
    }

    /**
     * El NIT identifica la cuenta que se liga a la venta. Los datos que el cajero escribe tienen
     * prioridad; los campos que deje vacíos se completan desde el perfil registrado.
     */
    public ClienteVenta cliente(Optional<VistaClienteEntidad> registrado) {
        String nombre = clienteNombre != null ? clienteNombre : registrado.map(VistaClienteEntidad::getNombre).orElse(null);
        String correo = clienteCorreo != null ? clienteCorreo : registrado.map(VistaClienteEntidad::getCorreo).orElse(null);
        return new ClienteVenta(registrado.map(VistaClienteEntidad::getUsuarioId).orElse(null), clienteNit, nombre,
                correo);
    }
}