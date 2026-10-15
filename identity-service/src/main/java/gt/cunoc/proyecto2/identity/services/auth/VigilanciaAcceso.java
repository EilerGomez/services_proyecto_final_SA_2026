package gt.cunoc.proyecto2.identity.services.auth;

import gt.cunoc.proyecto2.identity.models.CodigoDobleFactor;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CodigoDobleFactorRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra intentos fallidos en su propia transacción: el login que falla
 * termina en excepción (y rollback), así que el conteo no puede ir en ella.
 */
@Component
public class VigilanciaAcceso {

    static final int MAX_INTENTOS = 5;

    static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final UsuarioRepository usuarios;

    private final CodigoDobleFactorRepository desafios;

    public VigilanciaAcceso(UsuarioRepository usuarios, CodigoDobleFactorRepository desafios) {
        this.usuarios = usuarios;
        this.desafios = desafios;
    }

    /** Suma un fallo de contraseña; al quinto bloquea la cuenta. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void falloContrasena(UUID usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);
        if (usuario == null || bloqueadaPorAdministrador(usuario)) {
            return;   // un bloqueo del administrador no se convierte en uno temporal
        }
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= MAX_INTENTOS) {
            usuario.setEstado("BLOQUEADO");
            usuario.setBloqueadoHasta(Instant.now().plus(BLOQUEO));
            usuario.setIntentosFallidos(0);
        } else {
            usuario.setIntentosFallidos(intentos);
        }
        usuarios.save(usuario);
    }

    /**
     * Suma un fallo de código 2FA; al quinto bloquea la cuenta.
     *
     * @return true si la cuenta quedó bloqueada.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean falloCodigo(UUID desafioId, UUID usuarioId) {
        CodigoDobleFactor desafio = desafios.findById(desafioId).orElse(null);
        if (desafio == null) {
            return false;
        }
        int intentos = desafio.getIntentos() + 1;
        desafio.setIntentos(intentos);
        desafios.save(desafio);
        if (intentos < MAX_INTENTOS) {
            return false;
        }
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);
        if (usuario != null && !bloqueadaPorAdministrador(usuario)) {
            usuario.setEstado("BLOQUEADO");
            usuario.setBloqueadoHasta(Instant.now().plus(BLOQUEO));
            usuarios.save(usuario);
        }
        return true;
    }

    /** BLOQUEADO sin fecha = bloqueo puesto por un administrador (no vence). */
    private static boolean bloqueadaPorAdministrador(Usuario usuario) {
        return "BLOQUEADO".equals(usuario.getEstado()) && usuario.getBloqueadoHasta() == null;
    }
}
