package gt.cunoc.proyecto2.trips.repositorios.clientes;

import gt.cunoc.proyecto2.trips.modelos.clientes.VistaClienteEntidad;
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

    /**
     * T16: si el NIT que teclea la ventanilla es de una cuenta, el alquiler se liga a ella.
     * Sin distinguir mayúsculas: el NIT del perfil y el tecleado pueden traer la K escrita distinto.
     */
    Optional<VistaClienteEntidad> findFirstByNitIgnoreCase(String nit);

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
