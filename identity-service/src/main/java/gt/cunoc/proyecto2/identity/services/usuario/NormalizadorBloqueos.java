package gt.cunoc.proyecto2.identity.services.usuario;

import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quita los bloqueos temporales ya vencidos (estado BLOQUEADO con bloqueado_hasta en el pasado) sin
 * esperar a que el usuario vuelva a iniciar sesión.
 *
 * <p>Antes el bloqueo por intentos fallidos solo se limpiaba de forma perezosa al reintentar el login:
 * el panel, Fleet (R4c) y Wallet seguían viendo BLOQUEADO aunque ya hubiera vencido. Este normalizador
 * deja la cuenta ACTIVA en la base en cuanto pasa la fecha, para que todos vean el mismo estado.
 *
 * <p>El intervalo se configura con {@code app.cuentas.normalizar-bloqueos-ms} (por defecto 30 s).
 */
@Component
public class NormalizadorBloqueos {

    private static final Logger log = LoggerFactory.getLogger(NormalizadorBloqueos.class);

    private final UsuarioRepository usuarios;

    public NormalizadorBloqueos(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Reactiva las cuentas cuyo bloqueo temporal ya venció. Devuelve cuántas reactivó. */
    @Scheduled(fixedDelayString = "${app.cuentas.normalizar-bloqueos-ms:30000}")
    @Transactional
    public int normalizar() {
        List<Usuario> vencidos = usuarios.findByEstadoAndBloqueadoHastaBefore("BLOQUEADO", Instant.now());
        for (Usuario usuario : vencidos) {
            usuario.setEstado("ACTIVO");
            usuario.setBloqueadoHasta(null);
            usuario.setIntentosFallidos(0);
        }
        if (!vencidos.isEmpty()) {
            usuarios.saveAll(vencidos);
            log.info("Bloqueos temporales vencidos reactivados: {}", vencidos.size());
        }
        return vencidos.size();
    }
}
