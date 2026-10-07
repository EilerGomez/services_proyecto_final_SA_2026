package gt.cunoc.proyecto2.fleet.repositorios.sucursales;

import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SucursalRepositorio extends JpaRepository<SucursalEntidad, UUID> {

    List<SucursalEntidad> findAllByOrderByNombreAsc();

    List<SucursalEntidad> findByActivaOrderByNombreAsc(boolean activa);

    boolean existsByCodigo(String codigo);

    boolean existsByNombre(String nombre);

    /** Para editar: otro registro (distinto de id) ya usa ese nombre. */
    boolean existsByNombreAndIdNot(String nombre, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sucursal s WHERE s.id = :id")
    Optional<SucursalEntidad> bloquearPorId(@Param("id") UUID id);
}