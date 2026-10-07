package gt.cunoc.proyecto2.identity.messaging.publisher;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.identity.evento.payload.ClienteRegistradoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.CuentaCreadaPayload;
import gt.cunoc.proyecto2.identity.evento.payload.PerfilActualizadoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.RecuperacionContrasenaPayload;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UsuarioEventPublisher {

    private final PublicadorEventos publicador;

    public UsuarioEventPublisher(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    public void publicarCuentaCreada(
            UUID usuarioId,
            String correo,
            String nombre,
            List<String> roles,
            String urlActivacion,
            LocalDate fechaNegocio) {

        publicador.publicar("Usuario", usuarioId, "CuentaCreada",
                new CuentaCreadaPayload(usuarioId, correo, nombre, roles, urlActivacion), fechaNegocio);
    }

    public void publicarClienteRegistrado(
            UUID usuarioId,
            String nit,
            String nombre,
            String correo,
            LocalDate fechaNegocio) {

        publicador.publicar("Usuario", usuarioId, "ClienteRegistrado",
                new ClienteRegistradoPayload(usuarioId, nit, nombre, correo), fechaNegocio);
    }

    public void publicarRecuperacionContrasenaSolicitada(
            UUID usuarioId,
            String correo,
            String nombre,
            String urlRecuperacion,
            Instant expiraEn,
            LocalDate fechaNegocio) {

        publicador.publicar("Usuario", usuarioId, "RecuperacionContrasenaSolicitada",
                new RecuperacionContrasenaPayload(usuarioId, correo, nombre, urlRecuperacion, expiraEn), fechaNegocio);
    }

    public void publicarPerfilActualizado(
            UUID usuarioId,
            String nit,
            String nombres,
            String apellidos,
            String correo,
            String telefono,
            String fotoUrl,
            UUID choferId,
            LocalDate fechaNegocio) {

        publicador.publicar("Usuario", usuarioId, "PerfilActualizado",
                new PerfilActualizadoPayload(usuarioId, nit, nombres, apellidos, correo, telefono, fotoUrl, choferId), fechaNegocio);
    }
}
