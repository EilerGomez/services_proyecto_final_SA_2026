package gt.cunoc.proyecto2.sales.repositorios.asientos;

import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeEntidad;
import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeId;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Asientos de cada viaje. Los cambios de estado son UPDATE condicionales: solo toman asientos libres,
 * así dos ventas al mismo tiempo no se quedan con el mismo asiento (la que llega segunda actualiza menos
 * filas de las pedidas y responde 409). Libre = DISPONIBLE o una reserva ya vencida.
 */
@Repository
public interface AsientoViajeRepositorio extends JpaRepository<AsientoViajeEntidad, AsientoViajeId> {

    /** S1: todos los asientos del viaje en orden (1..capacidad). */
    List<AsientoViajeEntidad> findByViajeIdOrderByNumeroAsientoAsc(UUID viajeId);

    /** S4: los asientos libres pedidos pasan a VENDIDO. Devuelve cuántos cambió. */
    @Modifying
    @Query(value = """
            UPDATE asientos_viaje
               SET estado = 'VENDIDO', venta_id = :ventaId, reservado_hasta = NULL, version = version + 1
             WHERE viaje_id = :viajeId
               AND numero_asiento IN (:asientos)
               AND (estado = 'DISPONIBLE' OR (estado = 'RESERVADO' AND reservado_hasta < now()))""",
            nativeQuery = true)
    int vender(@Param("viajeId") UUID viajeId, @Param("asientos") Collection<Integer> asientos,
               @Param("ventaId") UUID ventaId);

    /** S2: los asientos libres pedidos quedan RESERVADO para la venta hasta la hora indicada. */
    @Modifying
    @Query(value = """
            UPDATE asientos_viaje
               SET estado = 'RESERVADO', venta_id = :ventaId, reservado_hasta = :hasta, version = version + 1
             WHERE viaje_id = :viajeId
               AND numero_asiento IN (:asientos)
               AND (estado = 'DISPONIBLE' OR (estado = 'RESERVADO' AND reservado_hasta < now()))""",
            nativeQuery = true)
    int reservar(@Param("viajeId") UUID viajeId, @Param("asientos") Collection<Integer> asientos,
                 @Param("ventaId") UUID ventaId, @Param("hasta") OffsetDateTime hasta);

    /** S5 (y luego rechazo o viaje eliminado): los asientos de la venta vuelven a DISPONIBLE. */
    @Modifying
    @Query(value = """
            UPDATE asientos_viaje
               SET estado = 'DISPONIBLE', venta_id = NULL, reservado_hasta = NULL, version = version + 1
             WHERE venta_id = :ventaId""",
            nativeQuery = true)
    int liberarDeVenta(@Param("ventaId") UUID ventaId);

    /** Tarea programada: libera las reservas que ya vencieron. */
    @Modifying
    @Query(value = """
            UPDATE asientos_viaje
               SET estado = 'DISPONIBLE', venta_id = NULL, reservado_hasta = NULL, version = version + 1
             WHERE estado = 'RESERVADO' AND reservado_hasta < now()""",
            nativeQuery = true)
    int liberarReservasVencidas();

    /**
     * ViajeProgramado / ViajeActualizado: crea los asientos 1..capacidad que falten.
     * Los que ya existen no se tocan (ON CONFLICT DO NOTHING), así un evento repetido no hace daño.
     */
    @Modifying
    @Query(value = """
            INSERT INTO asientos_viaje (viaje_id, numero_asiento)
            SELECT CAST(:viajeId AS uuid), n FROM generate_series(1, :capacidad) AS n
            ON CONFLICT DO NOTHING""",
            nativeQuery = true)
    int crearAsientos(@Param("viajeId") UUID viajeId, @Param("capacidad") int capacidad);

    /** CarteraDebitada: números de los asientos que la venta todavía tiene RESERVADO (vigentes o vencidos). */
    @Query(value = """
            SELECT numero_asiento FROM asientos_viaje
             WHERE venta_id = :ventaId AND estado = 'RESERVADO'
             ORDER BY numero_asiento""",
            nativeQuery = true)
    List<Integer> reservadosDeVenta(@Param("ventaId") UUID ventaId);

    /** CarteraDebitada: los asientos reservados de la venta pasan a VENDIDO. Devuelve cuántos cambió. */
    @Modifying
    @Query(value = """
            UPDATE asientos_viaje
               SET estado = 'VENDIDO', reservado_hasta = NULL, version = version + 1
             WHERE venta_id = :ventaId AND estado = 'RESERVADO'""",
            nativeQuery = true)
    int confirmarReservas(@Param("ventaId") UUID ventaId);
}
