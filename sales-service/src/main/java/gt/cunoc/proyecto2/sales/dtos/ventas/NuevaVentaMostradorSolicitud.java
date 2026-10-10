package gt.cunoc.proyecto2.sales.dtos.ventas;

import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.utilidades.Normalizador;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashSet;
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
        return validarAsientos(asientos, capacidad);
    }

    public ClienteVenta cliente(Optional<VistaClienteEntidad> registrado) {
        String nombre = clienteNombre != null ? clienteNombre : registrado.map(VistaClienteEntidad::getNombre).orElse(null);
        String correo = clienteCorreo != null ? clienteCorreo : registrado.map(VistaClienteEntidad::getCorreo).orElse(null);
        return new ClienteVenta(null, clienteNit, nombre, correo);
    }

    static List<Integer> validarAsientos(List<Integer> asientos, int capacidad) {
        if (new HashSet<>(asientos).size() != asientos.size()) {
            throw ErrorVenta.ASIENTOS_REPETIDOS.excepcion();
        }
        for (Integer numero : asientos) {
            if (numero > capacidad) {
                throw ErrorVenta.ASIENTO_FUERA_DE_RANGO.excepcion(capacidad, numero);
            }
        }
        return asientos.stream().sorted().toList();
    }
}