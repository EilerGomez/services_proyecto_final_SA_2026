package gt.cunoc.proyecto2.sales.repositorios.clientes;

import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Copia de clientes. Solo la escriben los manejadores de ClienteRegistrado y PerfilActualizado. */
@Repository
public interface VistaClienteRepositorio extends JpaRepository<VistaClienteEntidad, UUID> {

    /**
     * S4: si el NIT ya es de un cliente con cuenta, la venta se liga a esa cuenta y se completan su
     * nombre y correo. Sin distinguir mayúsculas: el NIT del perfil puede traer la K de otra forma
     * que la que teclea el cajero, y ahí se perdería la coincidencia.
     */
    Optional<VistaClienteEntidad> findFirstByNitIgnoreCase(String nit);

    /** Ventanilla: primeros clientes en orden alfabético, para elegir sin teclear el NIT completo. */
    List<VistaClienteEntidad> findAllByOrderByNombreAsc(Pageable pagina);

    /**
     * Ventanilla: clientes cuyo NIT, nombre o correo contienen el texto buscado. El patrón ya viene
     * en mayúsculas y con los comodines puestos (%TEXTO%).
     */
    @Query("""
            SELECT c FROM VistaCliente c
             WHERE upper(c.nit) LIKE :patron
                OR upper(c.nombre) LIKE :patron
                OR upper(c.correo) LIKE :patron
             ORDER BY c.nombre""")
    List<VistaClienteEntidad> buscar(@Param("patron") String patron, Pageable pagina);

    /** ClienteRegistrado / PerfilActualizado: upsert. Identity no manda version: gana el último. */
    @Modifying
    @Query(value = """
            INSERT INTO vista_clientes (usuario_id, nit, nombre, correo)
            VALUES (:usuarioId, :nit, :nombre, :correo)
            ON CONFLICT (usuario_id) DO UPDATE SET nit = EXCLUDED.nit, nombre = EXCLUDED.nombre,
                correo = EXCLUDED.correo""", nativeQuery = true)
    int guardar(@Param("usuarioId") UUID usuarioId, @Param("nit") String nit, @Param("nombre") String nombre,
                @Param("correo") String correo);
}
