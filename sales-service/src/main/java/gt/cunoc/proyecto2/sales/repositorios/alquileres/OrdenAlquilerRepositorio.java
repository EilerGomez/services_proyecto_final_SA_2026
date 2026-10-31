package gt.cunoc.proyecto2.sales.repositorios.alquileres;

import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenAlquilerRepositorio extends JpaRepository<OrdenAlquilerEntidad, UUID>,
        JpaSpecificationExecutor<OrdenAlquilerEntidad> {

    /** Historial por NIT: igual que en las ventas, sin distinguir mayúsculas. */
    List<OrdenAlquilerEntidad> findByClienteNitIgnoreCaseOrderByFechaCreacionDesc(String nit);

    List<OrdenAlquilerEntidad> findByClienteUsuarioIdOrderByFechaCreacionDesc(UUID clienteUsuarioId);

    /**
     * S10, "Mis órdenes": las de la cuenta y además las que están a su NIT sin cuenta enlazada.
     *
     * <p>Lo segundo hace falta porque una orden nace de CotizacionAlquilerConfirmada y copia el
     * clienteUsuarioId tal como venía en ese evento: si el alquiler se pidió en ventanilla antes de
     * que el cliente tuviera cuenta, la orden queda sin enlazar aunque después el alquiler sí se
     * enlace por NIT en Trips. Sin esto, el cliente ve su alquiler cancelado pero no su orden de pago.</p>
     *
     * <p>El NIT lo pone el servicio desde vista_clientes (copia de Identity), nunca el navegador, ya
     * en mayúsculas, y "CF" queda fuera porque no identifica a nadie. Null no hace falta descartarlo
     * aparte: la comparación con null da null y no agrega filas.</p>
     *
     * <p>El upper() va solo sobre la columna: PostgreSQL no puede resolver la función sobre un
     * parámetro cuyo tipo no conoce y responde "function upper(bytea) does not exist".</p>
     */
    @Query("""
            select o from OrdenAlquiler o
             where o.clienteUsuarioId = :usuarioId
                or (o.clienteUsuarioId is null and upper(o.clienteNit) = :nit)
             order by o.fechaCreacion desc""")
    List<OrdenAlquilerEntidad> mias(@Param("usuarioId") UUID usuarioId, @Param("nit") String nit);

    /**
     * ClienteRegistrado / PerfilActualizado: adopta las órdenes que quedaron sin cuenta y llevan su
     * NIT, igual que Trips hace con los alquileres. Así el cliente puede pagarlas con su billetera y
     * verlas en "Mis alquileres". El NIT viene del evento de Identity, no del navegador.
     *
     * @return cuántas órdenes quedaron enlazadas
     */
    @Modifying
    @Query(value = """
            UPDATE ordenes_alquiler
               SET cliente_usuario_id = :usuarioId
             WHERE cliente_usuario_id IS NULL
               AND upper(cliente_nit) = upper(:nit)""", nativeQuery = true)
    int enlazarPorNit(@Param("usuarioId") UUID usuarioId, @Param("nit") String nit);

    boolean existsByAlquilerId(UUID alquilerId);

    /** AlquilerCancelado: la orden del alquiler que Trips acaba de cancelar (alquiler_id es UNIQUE). */
    Optional<OrdenAlquilerEntidad> findByAlquilerId(UUID alquilerId);
}
