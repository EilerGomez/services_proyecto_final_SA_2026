package gt.cunoc.proyecto2.identity.messaging.publisher;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.identity.evento.payload.ClienteRegistradoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.CodigoDobleFactorPayload;
import gt.cunoc.proyecto2.identity.evento.payload.CuentaCreadaPayload;
import gt.cunoc.proyecto2.identity.evento.payload.PerfilActualizadoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.RecuperacionContrasenaPayload;
import gt.cunoc.proyecto2.identity.evento.payload.RolAsignadoPayload;
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

        publicarCuentaCreada(usuarioId, correo, nombre, roles, urlActivacion, null, fechaNegocio);
    }

    /** CuentaCreada de personal o chofer: incluye el nombre de la sucursal para el correo de bienvenida. */
    public void publicarCuentaCreada(
            UUID usuarioId,
            String correo,
            String nombre,
            List<String> roles,
            String urlActivacion,
            String sucursalNombre,
            LocalDate fechaNegocio) {

        publicador.publicar("Usuario", usuarioId, "CuentaCreada",
                new CuentaCreadaPayload(usuarioId, correo, nombre, roles, urlActivacion, sucursalNombre), fechaNegocio);
    }

    /** RolAsignado: una cuenta existente recibió ADMIN_SISTEMA, ADMIN_SUCURSAL o CAJERO. */
    public void publicarRolAsignado(
            UUID usuarioId,
            String correo,
            String nombre,
            String rol,
            UUID sucursalId,
            String sucursalNombre,
            LocalDate fechaNegocio) {

        publicador.publicar("Usuario", usuarioId, "RolAsignado",
                new RolAsignadoPayload(usuarioId, correo, nombre, rol, sucursalId, sucursalNombre), fechaNegocio);
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

    public void publicarCodigoDobleFactor(
            UUID usuarioId,
            String correo,
            String nombre,
            String codigo,
            Instant expiraEn,
            LocalDate fechaNegocio) {

        publicador.publicar("Usuario", usuarioId, "CodigoDobleFactorSolicitado",
                new CodigoDobleFactorPayload(usuarioId, correo, nombre, codigo, expiraEn), fechaNegocio);
    }
}
