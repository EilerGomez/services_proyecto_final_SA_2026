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
     * Cliente de la venta: lo que escribió el cajero manda; lo que falte se completa con el perfil
     * registrado. Si el NIT pertenece a una cuenta, la venta queda ligada a ella para que el
     * cliente pueda ver y reimprimir su boleto en "Mis boletos".
     */
    public ClienteVenta cliente(Optional<VistaClienteEntidad> registrado) {
        String nombre = clienteNombre != null ? clienteNombre : registrado.map(VistaClienteEntidad::getNombre).orElse(null);
        String correo = clienteCorreo != null ? clienteCorreo : registrado.map(VistaClienteEntidad::getCorreo).orElse(null);
        UUID usuarioId = registrado.map(VistaClienteEntidad::getUsuarioId).orElse(null);
        return new ClienteVenta(usuarioId, clienteNit, nombre, correo);
    }
}