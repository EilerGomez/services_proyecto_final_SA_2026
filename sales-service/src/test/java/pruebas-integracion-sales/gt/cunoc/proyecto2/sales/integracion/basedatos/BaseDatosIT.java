package gt.cunoc.proyecto2.sales.integracion.basedatos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.sales.servicios.ventas.VentaServicio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

/** Migraciones V1–V9, CHECK con nombre, índice único de boletos, cascada y triggers contra PostgreSQL 16 real. */
class BaseDatosIT extends PruebaIntegracion {

    @Autowired
    private VentaServicio ventas;

    private UUID xela;
    private UUID viaje;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
        viaje = insertarViaje(xela, 10, "PROGRAMADO");
    }

    /** INSERT de una venta con los valores dados (para probar cada CHECK). Devuelve el id. */
    private UUID insertarVentaCruda(String canal, UUID clienteUsuarioId, UUID cajeroUsuarioId, String metodoPago,
                                    Integer cantidadBoletos) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ventas (id, viaje_id, canal, cliente_usuario_id, sucursal_id, cajero_usuario_id, metodo_pago,
                                    monto_total, cantidad_boletos, fecha_pago, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, 75.00, ?, ?, 'CONFIRMADA')""",
                id, viaje, canal, clienteUsuarioId, xela, cajeroUsuarioId, metodoPago, cantidadBoletos, FECHA_PAGO);
        return id;
    }

    /** Venta de mostrador válida (PRESENCIAL, EFECTIVO, con cajero). */
    private UUID insertarVentaMostrador() {
        return insertarVentaCruda("PRESENCIAL", null, UUID.randomUUID(), "EFECTIVO", 1);
    }

    private void insertarBoleto(UUID ventaId, int asiento, String estado) {
        jdbc.update("""
                INSERT INTO boletos (venta_id, viaje_id, numero_asiento, precio, estado)
                VALUES (?, ?, ?, 75.00, ?)""", ventaId, viaje, asiento, estado);
    }

    private List<String> restricciones(String tabla) {
        return jdbc.queryForList("""
                SELECT conname FROM pg_constraint WHERE conrelid = CAST(? AS regclass) AND contype = 'c'""",
                String.class, tabla);
    }

    private int contar(String tabla) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
        return total == null ? 0 : total;
    }

    // ----- Flyway -----

    @Test
    void flywayAplicoLasNueveMigraciones() {
        List<String> versiones = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank",
                String.class);
        Integer fallidas = jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE NOT success",
                Integer.class);

        assertThat(versiones).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
        assertThat(fallidas).isZero();
    }

    @Test
    void lasRestriccionesConNombreExisten() {
        assertThat(restricciones("ventas")).contains("ck_ventas_cliente_en_linea", "ck_ventas_cajero_mostrador",
                "ck_ventas_metodo_pago", "ck_ventas_motivo_rechazo", "ck_ventas_cancelacion",
                "ck_ventas_cantidad_boletos");
        assertThat(restricciones("ordenes_alquiler")).contains("ck_ordenes_pago", "ck_ordenes_motivo_rechazo",
                "ck_ordenes_cartera_con_cuenta");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE indexname = 'ux_boleto_asiento_activo'",
                Integer.class)).isEqualTo(1);
    }

    // ----- CHECK de ventas (V5 y V8) -----

    @Test
    void losCheckDeVentasAlInsertar() {
        UUID cliente = UUID.randomUUID();

        assertThatThrownBy(() -> insertarVentaCruda("EN_LINEA", cliente, null, "EFECTIVO", 1))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ventas_metodo_pago");
        assertThatThrownBy(() -> insertarVentaCruda("PRESENCIAL", null, UUID.randomUUID(), "CARTERA", 1))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ventas_metodo_pago");
        assertThatThrownBy(() -> insertarVentaCruda("EN_LINEA", null, null, "CARTERA", 1))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ventas_cliente_en_linea");
        assertThatThrownBy(() -> insertarVentaCruda("PRESENCIAL", null, null, "EFECTIVO", 1))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ventas_cajero_mostrador");
        assertThatThrownBy(() -> insertarVentaCruda("PRESENCIAL", null, UUID.randomUUID(), "EFECTIVO", 0))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ventas_cantidad_boletos");
        assertThatThrownBy(() -> insertarVentaCruda("PRESENCIAL", null, UUID.randomUUID(), "EFECTIVO", null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("cantidad_boletos");                                 // NOT NULL (V8)
        assertThat(contar("ventas")).isZero();

        insertarVentaCruda("EN_LINEA", cliente, null, "CARTERA", 2);
        insertarVentaMostrador();
        assertThat(contar("ventas")).isEqualTo(2);
        // valores por omisión: consumidor final y version 0
        assertThat(jdbc.queryForList("SELECT DISTINCT cliente_nit || '|' || cliente_nombre || '|' || version FROM ventas",
                String.class)).containsExactly("CF|Consumidor final|0");
    }

    @Test
    void losCheckDeVentasAlCambiarDeEstado() {
        UUID venta = insertarVentaMostrador();

        assertThatThrownBy(() -> jdbc.update("UPDATE ventas SET estado = 'RECHAZADA' WHERE id = ?", venta))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ventas_motivo_rechazo");
        assertThatThrownBy(() -> jdbc.update("UPDATE ventas SET estado = 'CANCELADA', motivo_cancelacion = 'x' "
                + "WHERE id = ?", venta))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ventas_cancelacion");
        assertThatThrownBy(() -> jdbc.update("UPDATE ventas SET estado = 'VOLANDO' WHERE id = ?", venta))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE ventas SET monto_total = 0 WHERE id = ?", venta))
                .isInstanceOf(DataAccessException.class);
        assertThat(estadoVenta(venta)).isEqualTo("CONFIRMADA");

        jdbc.update("UPDATE ventas SET estado = 'CANCELADA', motivo_cancelacion = 'x', fecha_cancelacion = ? "
                + "WHERE id = ?", FECHA_PAGO, venta);
        assertThat(estadoVenta(venta)).isEqualTo("CANCELADA");
    }

    // ----- CHECK de órdenes de alquiler (V7) -----

    @Test
    void losCheckDeOrdenesDeAlquiler() {
        UUID deMostrador = insertarOrden(xela, null, "CF");
        UUID conCuenta = insertarOrden(xela, UUID.randomUUID(), "1234567-K");

        // pago con cartera solo si el alquiler tiene cuenta
        assertThatThrownBy(() -> jdbc.update("UPDATE ordenes_alquiler SET metodo_pago = 'CARTERA' WHERE id = ?",
                deMostrador))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ordenes_cartera_con_cuenta");
        // PAGADA sin método ni fecha
        assertThatThrownBy(() -> jdbc.update("UPDATE ordenes_alquiler SET estado = 'PAGADA' WHERE id = ?", deMostrador))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ordenes_pago");
        // RECHAZADA sin motivo
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE ordenes_alquiler SET estado = 'RECHAZADA', metodo_pago = 'CARTERA', fecha_pago = ?
                 WHERE id = ?""", FECHA_PAGO, conCuenta))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_ordenes_motivo_rechazo");
        // un alquiler tiene una sola orden
        UUID alquiler = jdbc.queryForObject("SELECT alquiler_id FROM ordenes_alquiler WHERE id = ?", UUID.class,
                conCuenta);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO ordenes_alquiler (alquiler_id, cliente_nombre, sucursal_id, origen, destino, monto)
                VALUES (?, 'Otro', ?, 'A', 'B', 10.00)""", alquiler, xela))
                .isInstanceOf(DataAccessException.class);

        assertThat(estadoOrden(deMostrador)).isEqualTo("PENDIENTE_PAGO");
        jdbc.update("UPDATE ordenes_alquiler SET metodo_pago = 'CARTERA', fecha_pago = ? WHERE id = ?",
                FECHA_PAGO, conCuenta);
        jdbc.update("UPDATE ordenes_alquiler SET estado = 'PAGADA', metodo_pago = 'EFECTIVO', fecha_pago = ? "
                + "WHERE id = ?", FECHA_PAGO, deMostrador);
        assertThat(estadoOrden(deMostrador)).isEqualTo("PAGADA");
    }

    // ----- Boletos y asientos (V4 y V6) -----

    @Test
    void unAsientoSoloTieneUnBoletoActivo() {
        UUID primera = insertarVentaMostrador();
        UUID segunda = insertarVentaMostrador();
        insertarBoleto(primera, 5, "ACTIVO");

        assertThatThrownBy(() -> insertarBoleto(segunda, 5, "ACTIVO"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ux_boleto_asiento_activo");
        // un boleto CANCELADO del mismo asiento no choca; al cancelar el primero se puede emitir otro
        insertarBoleto(segunda, 5, "CANCELADO");
        jdbc.update("UPDATE boletos SET estado = 'CANCELADO' WHERE venta_id = ?", primera);
        insertarBoleto(segunda, 5, "ACTIVO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM boletos WHERE numero_asiento = 5 AND estado = 'ACTIVO'",
                Integer.class)).isEqualTo(1);

        // el boleto necesita una venta (FK) y un asiento válido
        assertThatThrownBy(() -> insertarBoleto(UUID.randomUUID(), 6, "ACTIVO"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertarBoleto(primera, 0, "ACTIVO"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertarBoleto(primera, 7, "PERDIDO"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void losCheckDeAsientos() {
        // RESERVADO necesita venta y hora de vencimiento; VENDIDO necesita venta
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE asientos_viaje SET estado = 'RESERVADO', venta_id = ? WHERE viaje_id = ? AND numero_asiento = 1""",
                UUID.randomUUID(), viaje))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE asientos_viaje SET estado = 'VENDIDO' WHERE viaje_id = ? AND numero_asiento = 1", viaje))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE asientos_viaje SET estado = 'ROTO' WHERE viaje_id = ? AND numero_asiento = 1", viaje))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO asientos_viaje (viaje_id, numero_asiento) VALUES (?, 0)",
                viaje))
                .isInstanceOf(DataAccessException.class);
        // un viaje que no está en vista_viajes no tiene asientos (FK)
        assertThatThrownBy(() -> jdbc.update("INSERT INTO asientos_viaje (viaje_id, numero_asiento) VALUES (?, 1)",
                UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class);
        assertThat(estadosAsientos(viaje, 1)).containsExactly("DISPONIBLE");
    }

    @Test
    void borrarUnViajeBorraSusAsientosPeroConservaVentasYBoletos() {
        UUID otro = insertarViaje(xela, 5, "PROGRAMADO");
        UUID venta = insertarVentaMostrador();
        insertarBoleto(venta, 3, "ACTIVO");
        jdbc.update("UPDATE asientos_viaje SET estado = 'VENDIDO', venta_id = ? WHERE viaje_id = ? AND numero_asiento = 3",
                venta, viaje);

        jdbc.update("DELETE FROM vista_viajes WHERE viaje_id = ?", viaje);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM asientos_viaje WHERE viaje_id = ?", Integer.class, viaje))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM asientos_viaje WHERE viaje_id = ?", Integer.class, otro))
                .isEqualTo(5);
        assertThat(contar("ventas")).isEqualTo(1);
        assertThat(contar("boletos")).isEqualTo(1);
    }

    // ----- fn_actualizar_fecha (V1) -----

    @Test
    void elTriggerActualizaLaFechaDeModificacion() {
        UUID venta = insertarVentaMostrador();
        UUID orden = insertarOrden(xela, null, "CF");

        // aunque el UPDATE intente poner una fecha vieja, el trigger pone now()
        jdbc.update("UPDATE ventas SET fecha_actualizacion = TIMESTAMPTZ '2020-01-01 00:00:00+00' WHERE id = ?", venta);
        jdbc.update("UPDATE ordenes_alquiler SET fecha_actualizacion = TIMESTAMPTZ '2020-01-01 00:00:00+00' "
                + "WHERE id = ?", orden);

        assertThat(jdbc.queryForObject("SELECT fecha_actualizacion > TIMESTAMPTZ '2021-01-01 00:00:00+00' "
                + "AND fecha_actualizacion >= fecha_creacion FROM ventas WHERE id = ?", Boolean.class, venta)).isTrue();
        assertThat(jdbc.queryForObject("SELECT fecha_actualizacion > TIMESTAMPTZ '2021-01-01 00:00:00+00' "
                + "FROM ordenes_alquiler WHERE id = ?", Boolean.class, orden)).isTrue();
    }

    // ----- Consultas propias de los repositorios (tarea de reservas vencidas) -----

    @Test
    void lasReservasVencidasSeLiberanYSusVentasSeRechazan() {
        UUID vencida = insertarVentaCruda("EN_LINEA", UUID.randomUUID(), null, "CARTERA", 1);
        UUID reciente = insertarVentaCruda("EN_LINEA", UUID.randomUUID(), null, "CARTERA", 1);
        UUID viejaConReserva = insertarVentaCruda("EN_LINEA", UUID.randomUUID(), null, "CARTERA", 1);
        jdbc.update("UPDATE ventas SET estado = 'PENDIENTE_PAGO'");
        jdbc.update("UPDATE ventas SET fecha_creacion = now() - interval '10 minutes' WHERE id IN (?, ?)",
                vencida, viejaConReserva);
        jdbc.update("""
                UPDATE asientos_viaje SET estado = 'RESERVADO', venta_id = ?, reservado_hasta = now() - interval '1 minute'
                 WHERE viaje_id = ? AND numero_asiento = 1""", vencida, viaje);
        jdbc.update("""
                UPDATE asientos_viaje SET estado = 'RESERVADO', venta_id = ?, reservado_hasta = now() + interval '5 minutes'
                 WHERE viaje_id = ? AND numero_asiento = 2""", reciente, viaje);
        jdbc.update("""
                UPDATE asientos_viaje SET estado = 'RESERVADO', venta_id = ?, reservado_hasta = now() + interval '1 minute'
                 WHERE viaje_id = ? AND numero_asiento = 3""", viejaConReserva, viaje);

        int rechazadas = ventas.liberarReservasVencidas();

        assertThat(rechazadas).isEqualTo(1);
        assertThat(estadoVenta(vencida)).isEqualTo("RECHAZADA");
        assertThat(jdbc.queryForObject("SELECT motivo_rechazo || '|' || version FROM ventas WHERE id = ?", String.class,
                vencida)).isEqualTo("TIEMPO_AGOTADO|1");
        assertThat(estadoVenta(reciente)).isEqualTo("PENDIENTE_PAGO");
        assertThat(estadoVenta(viejaConReserva)).isEqualTo("PENDIENTE_PAGO");        // su reserva sigue vigente
        assertThat(estadosAsientos(viaje, 1, 2, 3)).containsExactly("DISPONIBLE", "RESERVADO", "RESERVADO");
        assertThat(jdbc.queryForObject("SELECT venta_id IS NULL AND reservado_hasta IS NULL FROM asientos_viaje "
                + "WHERE viaje_id = ? AND numero_asiento = 1", Boolean.class, viaje)).isTrue();
    }
}
