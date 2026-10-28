package gt.cunoc.proyecto2.trips.repositorios.tarifas;

import gt.cunoc.proyecto2.trips.modelos.tarifas.VistaTarifaReembolsoEntidad;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/* Copia de la política de cancelación de Sales. Solo la escribe el manejador de TarifaReembolsoCambiada. */
@Repository
public interface VistaTarifaReembolsoRepositorio extends JpaRepository<VistaTarifaReembolsoEntidad, UUID> {

    /**
     * Candidatas a cubrir una cancelación: el estado del alquiler o el comodín. La fila ganadora se
     * elige en el servicio (son pocas filas y la regla de desempate se lee mejor en Java que en SQL).
     */
    List<VistaTarifaReembolsoEntidad> findByActivaTrueAndEstadoAlquilerIn(List<String> estados);

    /** TarifaReembolsoCambiada: la tabla de Sales sí se edita, así que es un UPSERT por id. */
    @Modifying
    @Query(value = """
            INSERT INTO vista_tarifas_reembolso (id, estado_alquiler, horas_minimas_antes,
                                                 permite_cancelacion, porcentaje_reembolso,
                                                 justificacion, activa)
            VALUES (:id, :estado, :horas, :permite, :porcentaje, :justificacion, :activa)
            ON CONFLICT (id) DO UPDATE SET
                estado_alquiler      = EXCLUDED.estado_alquiler,
                horas_minimas_antes  = EXCLUDED.horas_minimas_antes,
                permite_cancelacion  = EXCLUDED.permite_cancelacion,
                porcentaje_reembolso = EXCLUDED.porcentaje_reembolso,
                justificacion        = EXCLUDED.justificacion,
                activa               = EXCLUDED.activa""", nativeQuery = true)
    int guardar(@Param("id") UUID id, @Param("estado") String estadoAlquiler, @Param("horas") int horasMinimasAntes,
                @Param("permite") boolean permiteCancelacion, @Param("porcentaje") BigDecimal porcentajeReembolso,
                @Param("justificacion") String justificacion, @Param("activa") boolean activa);
}
