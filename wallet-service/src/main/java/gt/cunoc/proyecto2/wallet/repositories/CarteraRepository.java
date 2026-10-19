package gt.cunoc.proyecto2.wallet.repositories;

import gt.cunoc.proyecto2.wallet.models.Cartera;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CarteraRepository extends JpaRepository<Cartera, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cartera> findWithLockByUsuarioId(UUID usuarioId);

    Optional<Cartera> findByUsuarioId(UUID usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cartera> findWithLockByTipo(String tipo);

    Optional<Cartera> findByTipo(String tipo);

    /**
     * Crea la billetera del sistema en cero si no existe (la migración V2 ya la crea; esto la repone si
     * alguien vació la tabla, p. ej. las pruebas). ON CONFLICT usa el índice único parcial ux_carteras_sistema.
     */
    @Modifying
    @Query(value = """
            INSERT INTO carteras (id, usuario_id, saldo, version, tipo)
            VALUES (gen_random_uuid(), NULL, 0, 0, 'SISTEMA')
            ON CONFLICT (tipo) WHERE tipo = 'SISTEMA' DO NOTHING""", nativeQuery = true)
    int crearSistemaSiNoExiste();

    /**
     * Crea la cartera en cero si el usuario no tiene. Idempotente aunque lleguen dos peticiones a la vez
     * (ON CONFLICT): no hace falta capturar errores de clave duplicada.
     *
     * @return 1 si la creó, 0 si ya existía
     */
    @Modifying
    @Query(value = """
            INSERT INTO carteras (id, usuario_id, saldo, version)
            VALUES (gen_random_uuid(), :usuarioId, 0, 0)
            ON CONFLICT (usuario_id) DO NOTHING""", nativeQuery = true)
    int crearSiNoExiste(@Param("usuarioId") UUID usuarioId);
}
