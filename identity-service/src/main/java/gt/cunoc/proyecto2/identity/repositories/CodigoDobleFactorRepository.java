package gt.cunoc.proyecto2.identity.repositories;


import gt.cunoc.proyecto2.identity.models.CodigoDobleFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;


public interface CodigoDobleFactorRepository extends JpaRepository<CodigoDobleFactor, UUID> {

    List<CodigoDobleFactor> findByUsuarioIdAndUsadoEnIsNullOrderByFechaCreacionDesc(UUID usuarioId);
}
