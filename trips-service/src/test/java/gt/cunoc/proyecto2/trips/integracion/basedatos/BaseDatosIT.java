package gt.cunoc.proyecto2.trips.integracion.basedatos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.trips.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.trips.modelos.tarifas.VistaTarifaDepreciacionEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.repositorios.tarifas.VistaTarifaDepreciacionRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

/** Migraciones V1–V13, triggers, CHECK y consultas propias de los repositorios contra PostgreSQL 16 real. */
class BaseDatosIT extends PruebaIntegracion {

    /** El nuevo viaje todavía no tiene id: el mismo valor que usa AsignacionViajeServicioImpl. */
    private static final UUID SIN_VIAJE = new UUID(0L, 0L);

    @Autowired
    private ViajeRepositorio viajes;
    @Autowired
    private VistaTarifaDepreciacionRepositorio tarifas;

    private UUID xela;
    private UUID ruta;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("Quetzaltenango", true);
        ruta = insertarRuta(xela, insertarSucursal("Guatemala", true), true);
    }

    private void insertarLlegada(UUID viajeId) {
        jdbc.update("""
                INSERT INTO llegadas_viaje (viaje_id, fecha_hora_llegada, km_final, km_recorridos, costo_combustible,
                                            tarifa_depreciacion, costo_depreciacion, salario_chofer, registrado_por)
                VALUES (?, ?, 1100.00, 100.00, 50.00, 0.5000, 50.00, 350.00, ?)""",
                viajeId, LLEGADA, UUID.randomUUID());
    }

    @Test
    void flywayAplicoTodasLasMigraciones() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL", Integer.class);

        assertThat(aplicadas).isEqualTo(15);
    }

    // ----- fn_registro_inmutable (V1) -----

    @Test
    void laConfiguracionDeAlquilerNoSeModificaNiSeElimina() {
        UUID precio = insertarPrecioAlquiler("12.50", LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> jdbc.update("UPDATE configuracion_alquiler SET precio_por_km = 99 WHERE id = ?",
                precio))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("no se pueden modificar ni eliminar");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM configuracion_alquiler WHERE id = ?", precio))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("configuracion_alquiler");
        assertThat(jdbc.queryForObject("SELECT precio_por_km FROM configuracion_alquiler WHERE id = ?",
                BigDecimal.class, precio)).isEqualByComparingTo("12.50");
    }

    @Test
    void lasSalidasYLlegadasNoSeModificanNiSeEliminan() {
        UUID bus = UUID.randomUUID();
        UUID chofer = UUID.randomUUID();
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "COMPLETADO");
        insertarSalida(viaje, bus, chofer, SALIDA, "1000.00");
        insertarLlegada(viaje);

        assertThatThrownBy(() -> jdbc.update("UPDATE salidas_viaje SET km_inicial = 0 WHERE viaje_id = ?", viaje))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("salidas_viaje");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM salidas_viaje WHERE viaje_id = ?", viaje))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE llegadas_viaje SET km_final = 0 WHERE viaje_id = ?", viaje))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("llegadas_viaje");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM llegadas_viaje WHERE viaje_id = ?", viaje))
                .isInstanceOf(DataAccessException.class);
        assertThat(contar("salidas_viaje")).isEqualTo(1);
        assertThat(contar("llegadas_viaje")).isEqualTo(1);
    }

    // ----- Triggers de viajes (V7) -----

    @Test
    void soloSeEliminanViajesProgramados() {
        UUID enRuta = insertarViajeRegular(ruta, xela, UUID.randomUUID(), UUID.randomUUID(), SALIDA, "EN_RUTA");
        UUID programado = insertarViajeRegular(ruta, xela, UUID.randomUUID(), UUID.randomUUID(), SALIDA, "PROGRAMADO");

        assertThatThrownBy(() -> jdbc.update("DELETE FROM viajes WHERE id = ?", enRuta))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Solo se pueden eliminar viajes que no han iniciado");
        assertThat(jdbc.update("DELETE FROM viajes WHERE id = ?", programado)).isEqualTo(1);
        assertThat(contar("viajes")).isEqualTo(1);
    }

    @Test
    void elTipoDeViajeNoSeModifica() {
        UUID viaje = insertarViajeRegular(ruta, xela, UUID.randomUUID(), UUID.randomUUID(), SALIDA, "PROGRAMADO");

        assertThatThrownBy(() -> jdbc.update("UPDATE viajes SET tipo = 'PRIVADO' WHERE id = ?", viaje))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("El tipo de viaje no se puede modificar");
        // otros cambios sí se permiten y fn_actualizar_fecha mueve fecha_actualizacion
        jdbc.update("UPDATE viajes SET fecha_actualizacion = TIMESTAMPTZ '2020-01-01 00:00:00+00' WHERE id = ?", viaje);
        assertThat(jdbc.queryForObject("SELECT fecha_actualizacion > TIMESTAMPTZ '2021-01-01 00:00:00+00' FROM viajes "
                + "WHERE id = ?", Boolean.class, viaje)).isTrue();
    }

    @Test
    void unaRutaConViajesNoSeBorraPorLaFk() {
        insertarViajeRegular(ruta, xela, UUID.randomUUID(), UUID.randomUUID(), SALIDA, "PROGRAMADO");

        assertThatThrownBy(() -> jdbc.update("DELETE FROM rutas WHERE id = ?", ruta))
                .isInstanceOf(DataAccessException.class);
    }

    // ----- CHECK de V4, V7 y V11 -----

    @Test
    void losCheckDeRutasYViajes() {
        assertThatThrownBy(() -> insertarRuta(xela, xela, true))                       // origen = destino
                .isInstanceOf(DataAccessException.class);
        UUID guate = jdbc.queryForObject("SELECT sucursal_destino_id FROM rutas WHERE id = ?", UUID.class, ruta);
        assertThatThrownBy(() -> insertarRuta(xela, guate, true))                      // UNIQUE origen-destino
                .isInstanceOf(DataAccessException.class);
        // REGULAR sin precio de boleto
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO viajes (tipo, ruta_id, sucursal_id, bus_id, chofer_id, capacidad_bus, salario_base_chofer,
                                    fecha_salida_programada, fecha_llegada_estimada, creado_por)
                VALUES ('REGULAR', ?, ?, ?, ?, 40, 350.00, ?, ?, ?)""",
                ruta, xela, UUID.randomUUID(), UUID.randomUUID(), SALIDA, LLEGADA, UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class);
        // llegada estimada antes de la salida
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO viajes (tipo, ruta_id, sucursal_id, bus_id, chofer_id, capacidad_bus, precio_boleto,
                                    salario_base_chofer, fecha_salida_programada, fecha_llegada_estimada, creado_por)
                VALUES ('REGULAR', ?, ?, ?, ?, 40, 75.00, 350.00, ?, ?, ?)""",
                ruta, xela, UUID.randomUUID(), UUID.randomUUID(), LLEGADA, SALIDA, UUID.randomUUID()))
                .isInstanceOf(DataAccessException.class);
        assertThat(contar("viajes")).isZero();
    }

    @Test
    void losCheckDeAlquileres() {
        UUID alquiler = insertarAlquiler(xela, UUID.randomUUID(), "SOLICITADO");

        // COTIZADO sin precio confirmado
        assertThatThrownBy(() -> jdbc.update("UPDATE alquileres_privados SET estado = 'COTIZADO' WHERE id = ?",
                alquiler))
                .isInstanceOf(DataAccessException.class);
        // CANCELADO sin motivo
        assertThatThrownBy(() -> jdbc.update("UPDATE alquileres_privados SET estado = 'CANCELADO' WHERE id = ?",
                alquiler))
                .isInstanceOf(DataAccessException.class);
        // EN_LINEA sin cliente
        assertThatThrownBy(() -> jdbc.update("UPDATE alquileres_privados SET cliente_usuario_id = NULL WHERE id = ?",
                alquiler))
                .isInstanceOf(DataAccessException.class);
        // un viaje PRIVADO necesita un alquiler que exista (FK de V11)
        assertThatThrownBy(() -> insertarViajePrivado(UUID.randomUUID(), xela, UUID.randomUUID(), UUID.randomUUID(),
                SALIDA, "PROGRAMADO"))
                .isInstanceOf(DataAccessException.class);
        // coordenadas del recorrido (V13): latitud y longitud van juntas y dentro de rango
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE alquileres_privados SET origen_latitud = 14.8347 WHERE id = ?", alquiler))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE alquileres_privados SET destino_latitud = 91, destino_longitud = -91 WHERE id = ?", alquiler))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.update(
                "UPDATE alquileres_privados SET origen_latitud = 14.8347, origen_longitud = -91.5184 WHERE id = ?",
                alquiler)).isEqualTo(1);
        assertThat(estadoDelAlquiler(alquiler)).isEqualTo("SOLICITADO");
    }

    // ----- Consultas propias de los repositorios -----

    @Test
    void choquesDeHorarioDeBusYChofer() {
        UUID bus = UUID.randomUUID();
        UUID chofer = UUID.randomUUID();
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");      // 06:00–10:00
        insertarViajeRegular(ruta, xela, bus, chofer, SALIDA.plusDays(1), "COMPLETADO");
        OffsetDateTime antes = SALIDA.minusHours(4);

        assertThat(viajes.existeChoqueDeBus(bus, SIN_VIAJE, EstadoViaje.ACTIVOS, SALIDA.plusHours(3), LLEGADA.plusHours(3)))
                .isTrue();
        assertThat(viajes.existeChoqueDeBus(bus, SIN_VIAJE, EstadoViaje.ACTIVOS, antes, SALIDA)).isFalse();   // termina justo
        assertThat(viajes.existeChoqueDeBus(bus, viaje, EstadoViaje.ACTIVOS, SALIDA, LLEGADA)).isFalse();     // él mismo
        assertThat(viajes.existeChoqueDeChofer(chofer, SIN_VIAJE, EstadoViaje.ACTIVOS, SALIDA, LLEGADA)).isTrue();
        assertThat(viajes.existeChoqueDeChofer(chofer, SIN_VIAJE, EstadoViaje.ACTIVOS,
                SALIDA.plusDays(1), LLEGADA.plusDays(1))).isFalse();                                          // COMPLETADO
        assertThat(viajes.existsByRutaId(ruta)).isTrue();
    }

    @Test
    void laTarifaVigenteEsLaMasRecienteQueNoPasaDeLaFecha() {
        insertarTarifa("0.5000", LocalDate.of(2026, 1, 1));
        insertarTarifa("0.7500", LocalDate.of(2026, 11, 1));
        insertarTarifa("0.9000", LocalDate.of(2026, 12, 1));

        Optional<VistaTarifaDepreciacionEntidad> vigente =
                tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(DIA);

        assertThat(vigente).isPresent();
        assertThat(vigente.get().getMontoPorKm()).isEqualByComparingTo("0.75");
        assertThat(tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(LocalDate.of(2025, 12, 31)))
                .isEmpty();
        // vigente_desde es UNIQUE: otra tarifa con la misma fecha falla
        assertThatThrownBy(() -> insertarTarifa("1.0000", LocalDate.of(2026, 1, 1)))
                .isInstanceOf(DataAccessException.class);
    }
}
