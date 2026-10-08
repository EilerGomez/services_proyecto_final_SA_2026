package gt.cunoc.proyecto2.identity.repositories;

import gt.cunoc.proyecto2.identity.models.SolicitudCuentaPersonal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SolicitudCuentaRepository extends JpaRepository<SolicitudCuentaPersonal, UUID> {

}
