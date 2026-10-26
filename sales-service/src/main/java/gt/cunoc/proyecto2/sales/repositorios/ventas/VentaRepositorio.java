package gt.cunoc.proyecto2.sales.repositorios.ventas;

import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Ventas. Las búsquedas con filtros opcionales (S6) usan EspecificacionesVenta. */
@Repository
public interface VentaRepositorio extends JpaRepository<VentaEntidad, UUID>, JpaSpecificationExecutor<VentaEntidad> {

    /**
     * La más reciente primero: por fecha de pago y, el mismo día, por el momento en que se registró.
     * El id solo desempata dos ventas registradas en el mismo instante (así el orden siempre es el mismo).
     */
    Sort RECIENTES_PRIMERO = Sort.by(
            Sort.Order.desc("fechaPago"), Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id"));

    /** Historial por NIT (S7, S8): incluye las compras en mostrador hechas con el mismo NIT. */
    List<VentaEntidad> findByClienteNit(String nit, Sort orden);

    /**
     * Tarea programada: ventas en línea que llevan más de los minutos de reserva sin pagarse y que ya no
     * tienen ninguna reserva vigente → RECHAZADA con TIEMPO_AGOTADO. Si después llega su CarteraDebitada,
     * el manejador pide el reembolso (débito tardío). Se llama ANTES de liberar los asientos.
     */
    @Modifying
    @Query(value = """
            UPDATE ventas v
               SET estado = 'RECHAZADA', motivo_rechazo = 'TIEMPO_AGOTADO', version = v.version + 1
             WHERE v.estado = 'PENDIENTE_PAGO'
               AND v.canal = 'EN_LINEA'
               AND v.fecha_creacion < now() - make_interval(mins => :minutos)
               AND NOT EXISTS (SELECT 1 FROM asientos_viaje a
                                WHERE a.venta_id = v.id AND a.estado = 'RESERVADO' AND a.reservado_hasta >= now())""",
            nativeQuery = true)
    int rechazarPendientesVencidas(@Param("minutos") int minutos);

    /**
     * Saga (CarteraDebitada, DebitoCarteraRechazado): lee la venta con SELECT ... FOR UPDATE.
     * La tarea de reservas vencidas también bloquea primero la venta y luego los asientos: con el mismo
     * orden no se cruzan (sin deadlock) y la segunda en llegar ve el estado que dejó la primera.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<VentaEntidad> buscarParaActualizar(@Param("id") UUID id);

    /** ViajeEliminado: ventas del viaje que hay que rechazar (pendientes) o cancelar (confirmadas). */
    List<VentaEntidad> findByViajeIdAndEstadoIn(UUID viajeId, Collection<EstadoVenta> estados);

    /**
     * S14: venta cuyo boleto impreso lleva este código de barras (los primeros 12 caracteres del id,
     * en mayúsculas). Usa el índice funcional ix_ventas_codigo de V11, que no es único: devuelve una
     * lista porque dos ids podrían empezar igual, y es mejor mostrar las dos que esconder una.
     */
    @Query(value = """
            SELECT * FROM ventas
             WHERE upper(substr(replace(id::text, '-', ''), 1, 12)) = :codigo
             ORDER BY fecha_creacion DESC""",
            nativeQuery = true)
    List<VentaEntidad> buscarPorCodigo(@Param("codigo") String codigo);
}
