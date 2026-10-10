package gt.cunoc.proyecto2.trips.repositorios.choferes;

import gt.cunoc.proyecto2.trips.modelos.choferes.VistaChoferEntidad;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/* Consultas de la copia de choferes. Solo la escriben los manejadores de eventos (guardar, desactivar). */
@Repository
public interface VistaChoferRepositorio extends JpaRepository<VistaChoferEntidad, UUID> {

    /** ChoferCreado / ChoferActualizado: upsert con el estado completo; ignora versiones viejas (devuelve 0). */
    @Modifying
    @Query(value = """
            INSERT INTO vista_choferes (id, sucursal_id, nombre_completo, salario_base_viaje, estado, version)
            VALUES (:id, :sucursalId, :nombre, :salario, :estado, :version)
            ON CONFLICT (id) DO UPDATE SET sucursal_id = EXCLUDED.sucursal_id,
                nombre_completo = EXCLUDED.nombre_completo, salario_base_viaje = EXCLUDED.salario_base_viaje,
                estado = EXCLUDED.estado, version = EXCLUDED.version
            WHERE vista_choferes.version <= EXCLUDED.version""", nativeQuery = true)
    int guardar(@Param("id") UUID id, @Param("sucursalId") UUID sucursalId, @Param("nombre") String nombre,
                @Param("salario") BigDecimal salario, @Param("estado") String estado,
                @Param("version") long version);

    /** ChoferDesactivado: estado INACTIVO si el evento no es viejo (devuelve 0 si no cambió). */
    @Modifying
    @Query(value = """
            UPDATE vista_choferes SET estado = 'INACTIVO', version = :version
            WHERE id = :id AND version <= :version""", nativeQuery = true)
    int desactivar(@Param("id") UUID id, @Param("version") long version);
}