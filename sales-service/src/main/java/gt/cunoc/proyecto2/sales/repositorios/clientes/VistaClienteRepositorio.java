package gt.cunoc.proyecto2.sales.repositorios.clientes;

import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Copia de clientes. Solo la escriben los manejadores de ClienteRegistrado y PerfilActualizado. */
@Repository
public interface VistaClienteRepositorio extends JpaRepository<VistaClienteEntidad, UUID> {

    /** S4: si el NIT ya es de un cliente con cuenta, se completan su nombre y correo. */
    Optional<VistaClienteEntidad> findFirstByNit(String nit);

    /** S4: clientes sugeridos para la búsqueda del cajero, acotados para no cargar toda la tabla. */
    List<VistaClienteEntidad> findTop20ByOrderByNombreAsc();

    /** S4: búsqueda parcial por NIT, nombre o correo. */
    List<VistaClienteEntidad> findTop20ByNitContainingIgnoreCaseOrNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc(
            String nit, String nombre, String correo);

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
