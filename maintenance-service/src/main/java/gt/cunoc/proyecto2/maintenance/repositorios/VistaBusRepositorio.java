package gt.cunoc.proyecto2.maintenance.repositorios;

import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/* Copia local de buses. Solo la escriben los manejadores de eventos (guardar, desactivar). */
public interface VistaBusRepositorio extends JpaRepository<VistaBusEntidad, UUID> {

    List<VistaBusEntidad> findBySucursalIdOrderByPlacaAsc(UUID sucursalId);

    /** BusCreado / BusActualizado: upsert con el estado completo; ignora versiones viejas (devuelve 0). */
    @Modifying
    @Query(value = """
            INSERT INTO vista_buses (bus_id, placa, sucursal_id, activo, version)
            VALUES (:busId, :placa, :sucursalId, :activo, :version)
            ON CONFLICT (bus_id) DO UPDATE SET placa = EXCLUDED.placa, sucursal_id = EXCLUDED.sucursal_id,
                activo = EXCLUDED.activo, version = EXCLUDED.version
            WHERE vista_buses.version <= EXCLUDED.version""", nativeQuery = true)
    int guardar(@Param("busId") UUID busId, @Param("placa") String placa, @Param("sucursalId") UUID sucursalId,
                @Param("activo") boolean activo, @Param("version") long version);

    /** BusDesactivado: activo = false si el evento no es viejo (devuelve 0 si no cambió o no existe). */
    @Modifying
    @Query(value = """
            UPDATE vista_buses SET activo = false, version = :version
            WHERE bus_id = :busId AND version <= :version""", nativeQuery = true)
    int desactivar(@Param("busId") UUID busId, @Param("version") long version);
}
