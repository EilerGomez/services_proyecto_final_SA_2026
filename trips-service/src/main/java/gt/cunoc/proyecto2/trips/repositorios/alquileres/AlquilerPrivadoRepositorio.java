package gt.cunoc.proyecto2.trips.repositorios.alquileres;

import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlquilerPrivadoRepositorio extends JpaRepository<AlquilerPrivadoEntidad, UUID>,
        JpaSpecificationExecutor<AlquilerPrivadoEntidad> {

    /** T18: los del cliente, de la salida más lejana a la más antigua. */
    List<AlquilerPrivadoEntidad> findByClienteUsuarioIdOrderByFechaSalidaDesc(UUID clienteUsuarioId);

    /**
     * ClienteRegistrado / PerfilActualizado: adopta los alquileres de ventanilla que se pidieron con
     * este NIT antes de que la cuenta existiera (o antes de que el perfil tuviera NIT). Así el cliente
     * los ve en "Mis alquileres", que filtra por cliente_usuario_id.
     *
     * <p>El NIT viene del evento de Identity, no del navegador: nadie puede reclamar alquileres ajenos
     * escribiendo un NIT. Solo toca los que no tienen cuenta (que por el CHECK de la tabla son los
     * presenciales) y no sube la version, porque el alquiler no cambió para quien lo esté editando.</p>
     *
     * @return cuántos alquileres quedaron enlazados
     */
    @Modifying
    @Query(value = """
            UPDATE alquileres_privados
               SET cliente_usuario_id = :usuarioId
             WHERE cliente_usuario_id IS NULL
               AND upper(cliente_nit) = upper(:nit)""", nativeQuery = true)
    int enlazarPorNit(@Param("usuarioId") UUID usuarioId, @Param("nit") String nit);
}