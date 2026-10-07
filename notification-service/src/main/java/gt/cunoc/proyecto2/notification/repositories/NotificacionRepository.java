package gt.cunoc.proyecto2.notification.repositories;


import gt.cunoc.proyecto2.notification.models.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    Optional<Notificacion> findByEventoIdAndDestinatarioAndPlantilla(
            UUID eventoId, String destinatario, String plantilla);
}
