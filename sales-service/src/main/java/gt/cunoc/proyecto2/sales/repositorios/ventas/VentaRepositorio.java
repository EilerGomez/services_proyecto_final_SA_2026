package gt.cunoc.proyecto2.sales.repositorios.ventas;

import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaRepositorio extends JpaRepository<VentaEntidad, UUID>, JpaSpecificationExecutor<VentaEntidad> {

    List<VentaEntidad> findByClienteNit(String nit, Sort orden);

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
}