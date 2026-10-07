package gt.cunoc.proyecto2.fleet.repositorios.personal;

import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonalSucursalRepositorio extends JpaRepository<PersonalSucursalEntidad, UUID> {

    /** F6. rol y activo son filtros opcionales (null = sin filtro). */
    @Query("""
            SELECT p FROM PersonalSucursal p
            WHERE p.sucursalId = :sucursalId
              AND (:rol IS NULL OR p.rol = :rol)
              AND (:activo IS NULL OR p.activo = :activo)
            ORDER BY p.activo DESC, p.nombreCompleto ASC""")
    List<PersonalSucursalEntidad> buscar(@Param("sucursalId") UUID sucursalId,
                                         @Param("rol") RolPersonal rol,
                                         @Param("activo") Boolean activo);

    boolean existsBySucursalIdAndRolAndActivoTrueAndIdNot(UUID sucursalId, RolPersonal rol, UUID id);

    /**
     * Evento PerfilActualizado: copia de nombre y correo en todas las filas de la persona (activas o no).
     * Es solo una copia de Identity: no sube la version ni publica eventos.
     */
    @Modifying
    @Query("""
            UPDATE PersonalSucursal p
            SET p.nombreCompleto = :nombreCompleto, p.correo = :correo
            WHERE p.usuarioId = :usuarioId""")
    int actualizarDatosDePerfil(@Param("usuarioId") UUID usuarioId,
                                @Param("nombreCompleto") String nombreCompleto,
                                @Param("correo") String correo);
}