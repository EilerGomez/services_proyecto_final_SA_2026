package gt.cunoc.proyecto2.trips.repositorios.tarifas;

import gt.cunoc.proyecto2.trips.modelos.tarifas.VistaTarifaDepreciacionEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/* Consultas de la copia de tarifas. Solo la escribe el manejador de TarifaDepreciacionCambiada (guardar). */
@Repository
public interface VistaTarifaDepreciacionRepositorio extends JpaRepository<VistaTarifaDepreciacionEntidad, UUID> {

    /** Tarifa vigente en una fecha: la de vigente_desde más reciente que no pase de esa fecha. */
    Optional<VistaTarifaDepreciacionEntidad> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
            LocalDate fecha);

    /** TarifaDepreciacionCambiada: las tarifas no se editan, así que solo se inserta (0 si ya existía). */
    @Modifying
    @Query(value = """
            INSERT INTO vista_tarifas_depreciacion (id, monto_por_km, vigente_desde)
            VALUES (:id, :montoPorKm, :vigenteDesde)
            ON CONFLICT (id) DO NOTHING""", nativeQuery = true)
    int guardar(@Param("id") UUID id, @Param("montoPorKm") BigDecimal montoPorKm,
                @Param("vigenteDesde") LocalDate vigenteDesde);
}