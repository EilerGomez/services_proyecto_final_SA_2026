package gt.cunoc.proyecto2.trips.repositorios.buses;

import gt.cunoc.proyecto2.trips.modelos.buses.VistaBusEntidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/* Consultas de la copia de buses. Solo la escriben los manejadores de eventos (guardar, desactivar). */
@Repository
public interface VistaBusRepositorio extends JpaRepository<VistaBusEntidad, UUID> {

    /** BusCreado / BusActualizado: upsert con el estado completo; ignora versiones viejas y nos (devuelve 0). */
    @Modifying
    @Query(value = """
            INSERT INTO vista_buses (id, sucursal_id, placa, capacidad_pasajeros, estado, version)
            VALUES (:id, :sucursalId, :placa, :capacidad, :estado, :version)
            ON CONFLICT (id) DO UPDATE SET sucursal_id = EXCLUDED.sucursal_id, placa = EXCLUDED.placa,
                capacidad_pasajeros = EXCLUDED.capacidad_pasajeros, estado = EXCLUDED.estado,
                version = EXCLUDED.version
            WHERE vista_buses.version <= EXCLUDED.version""", nativeQuery = true)
    int guardar(@Param("id") UUID id, @Param("sucursalId") UUID sucursalId, @Param("placa") String placa,
                @Param("capacidad") int capacidad, @Param("estado") String estado, @Param("version") long version);

    /** BusDesactivado: estado INACTIVO si el evento no es viejo (devuelve 0 si no cambió). */
    @Modifying
    @Query(value = """
            UPDATE vista_buses SET estado = 'INACTIVO', version = :version
            WHERE id = :id AND version <= :version""", nativeQuery = true)
    int desactivar(@Param("id") UUID id, @Param("version") long version);
}