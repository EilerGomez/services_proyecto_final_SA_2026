package gt.cunoc.proyecto2.trips.repositorios.sucursales;

import gt.cunoc.proyecto2.trips.modelos.sucursales.VistaSucursalEntidad;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/* Consultas de la copia de sucursales. Solo la escriben los manejadores de eventos (guardar). */
@Repository
public interface VistaSucursalRepositorio extends JpaRepository<VistaSucursalEntidad, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO vista_sucursales (id, nombre, latitud, longitud, activa, version)
            VALUES (:id, :nombre, :latitud, :longitud, :activa, :version)
            ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre, latitud = EXCLUDED.latitud,
                longitud = EXCLUDED.longitud, activa = EXCLUDED.activa, version = EXCLUDED.version
            WHERE vista_sucursales.version <= EXCLUDED.version""", nativeQuery = true)
    int guardar(@Param("id") UUID id, @Param("nombre") String nombre, @Param("latitud") BigDecimal latitud,
                @Param("longitud") BigDecimal longitud, @Param("activa") boolean activa,
                @Param("version") long version);
}