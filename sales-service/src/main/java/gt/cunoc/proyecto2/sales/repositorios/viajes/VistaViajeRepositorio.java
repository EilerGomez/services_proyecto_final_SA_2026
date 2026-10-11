package gt.cunoc.proyecto2.sales.repositorios.viajes;

import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Copia local de los viajes REGULAR. Solo la escriben los manejadores de eventos (guardar, iniciar, eliminar),
 * siempre con "version <=" para ignorar eventos viejos que lleguen tarde (devuelven 0 filas).
 */
@Repository
public interface VistaViajeRepositorio extends JpaRepository<VistaViajeEntidad, UUID> {

    /** ViajeProgramado / ViajeActualizado: upsert con el estado completo; el viaje queda PROGRAMADO. */
    @Modifying
    @Query(value = """
            INSERT INTO vista_viajes (viaje_id, ruta_id, sucursal_origen_id, origen_nombre, destino_nombre,
                                      fecha_salida, bus_id, bus_placa, capacidad, precio_boleto, estado, version)
            VALUES (:viajeId, :rutaId, :sucursalId, :origen, :destino, :fechaSalida, :busId, :busPlaca,
                    :capacidad, :precio, 'PROGRAMADO', :version)
            ON CONFLICT (viaje_id) DO UPDATE SET ruta_id = EXCLUDED.ruta_id,
                sucursal_origen_id = EXCLUDED.sucursal_origen_id, origen_nombre = EXCLUDED.origen_nombre,
                destino_nombre = EXCLUDED.destino_nombre, fecha_salida = EXCLUDED.fecha_salida,
                bus_id = EXCLUDED.bus_id, bus_placa = EXCLUDED.bus_placa, capacidad = EXCLUDED.capacidad,
                precio_boleto = EXCLUDED.precio_boleto, estado = EXCLUDED.estado, version = EXCLUDED.version
            WHERE vista_viajes.version <= EXCLUDED.version""", nativeQuery = true)
    int guardar(@Param("viajeId") UUID viajeId, @Param("rutaId") UUID rutaId, @Param("sucursalId") UUID sucursalId,
                @Param("origen") String origen, @Param("destino") String destino,
                @Param("fechaSalida") OffsetDateTime fechaSalida, @Param("busId") UUID busId,
                @Param("busPlaca") String busPlaca, @Param("capacidad") int capacidad,
                @Param("precio") BigDecimal precio, @Param("version") long version);

    /** ViajeIniciado: EN_RUTA, ya no se vende. */
    @Modifying
    @Query(value = """
            UPDATE vista_viajes SET estado = 'EN_RUTA', version = :version
            WHERE viaje_id = :viajeId AND version <= :version""", nativeQuery = true)
    int iniciar(@Param("viajeId") UUID viajeId, @Param("version") long version);

    /** ViajeEliminado: borra el viaje (sus asientos se van en cascada; ventas y boletos se conservan). */
    @Modifying
    @Query(value = "DELETE FROM vista_viajes WHERE viaje_id = :viajeId AND version <= :version",
            nativeQuery = true)
    int eliminar(@Param("viajeId") UUID viajeId, @Param("version") long version);
}
