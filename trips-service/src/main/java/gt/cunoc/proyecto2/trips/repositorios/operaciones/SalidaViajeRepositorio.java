package gt.cunoc.proyecto2.trips.repositorios.operaciones;

import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** El id es el viajeId. */
@Repository
public interface SalidaViajeRepositorio extends JpaRepository<SalidaViajeEntidad, UUID> {

    /**
     * T14. El kilometraje más alto con el que salió este bus. Se usa junto al odómetro que manda
     * Fleet: el evento del último viaje puede venir en camino y esta tabla ya lo sabe. Null si el
     * bus nunca ha salido desde aquí.
     */
    @Query("SELECT max(s.kmInicial) FROM SalidaViaje s WHERE s.busId = :busId")
    BigDecimal ultimoKilometrajeDelBus(@Param("busId") UUID busId);
}