package gt.cunoc.proyecto2.fleet.integracion.basedatos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.fleet.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.fleet.modelos.bitacora.BitacoraKilometrajeEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import gt.cunoc.proyecto2.fleet.repositorios.bitacora.BitacoraKilometrajeRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.buses.BusRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.utilidades.Fechas;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Migraciones de Flyway, triggers y consultas propias de los repositorios contra PostgreSQL 16 real. */
class BaseDatosIT extends PruebaIntegracion {

    @Autowired
    private BitacoraKilometrajeRepositorio bitacora;
    @Autowired
    private BusRepositorio buses;
    @Autowired
    private SucursalRepositorio sucursales;
    @Autowired
    private PlatformTransactionManager transacciones;

    @Test
    void flywayAplicoLasSieteMigraciones() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL", Integer.class);

        assertThat(aplicadas).isEqualTo(7);
    }

    @Test
    void losRegistrosNoSeEliminan() {
        UUID xela = insertarSucursal("XELA", true);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM sucursales WHERE id = ?", xela))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("no se eliminan");
    }

    @Test
    void elTriggerProtegeAlUltimoAdminDeUnaSucursalActiva() {
        UUID xela = insertarSucursal("XELA", true);
        UUID admin = insertarPersonal(xela, "ADMIN_SUCURSAL");

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE personal_sucursal SET activo = false, fecha_desasignacion = DATE '2026-10-20' WHERE id = ?",
                admin))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("al menos un administrador activo");
    }

    @Test
    void soloUnaAsignacionActivaPorUsuario() {
        UUID xela = insertarSucursal("XELA", true);
        UUID guate = insertarSucursal("GUATE", true);
        UUID admin = insertarPersonal(xela, "CAJERO");
        UUID usuario = jdbc.queryForObject("SELECT usuario_id FROM personal_sucursal WHERE id = ?", UUID.class, admin);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO personal_sucursal (id, sucursal_id, usuario_id, nombre_completo, correo, rol_codigo)
                VALUES (?, ?, ?, 'Ana', 'ana@fleet.gt', 'CAJERO')""", UUID.randomUUID(), guate, usuario))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void bitacoraPorRangoOrdenadaDelMasRecienteAlMasViejo() {
        UUID bus = insertarBus(insertarSucursal("XELA", true), "P-111AAA", "ACTIVO", "300");
        insertarViajeEnBitacora(bus, "0", "100", OffsetDateTime.parse("2026-10-05T10:00:00-06:00"));
        insertarViajeEnBitacora(bus, "100", "300", OffsetDateTime.parse("2026-10-06T10:00:00-06:00"));

        List<BitacoraKilometrajeEntidad> viajes = bitacora.buscarPorRango(bus,
                Fechas.inicioDelDia(LocalDate.of(2026, 10, 1)), Fechas.finDelDia(LocalDate.of(2026, 10, 31)));

        assertThat(viajes).extracting(v -> v.kmRecorridos().intValue()).containsExactly(200, 100);
        assertThat(bitacora.findTop5ByBusIdOrderByFechaLlegadaDesc(bus)).hasSize(2);
        assertThat(bitacora.existsByViajeId(viajes.get(0).getViajeId())).isTrue();
    }

    @Test
    void buscarBusesConYSinFiltros() {
        UUID xela = insertarSucursal("XELA", true);
        insertarBus(xela, "P-222BBB", "EN_TALLER", "0");
        insertarBus(xela, "P-111AAA", "ACTIVO", "0");

        assertThat(buses.buscar(null, null)).extracting("placa").containsExactly("P-111AAA", "P-222BBB");
        assertThat(buses.buscar(xela, EstadoBus.EN_TALLER)).extracting("placa").containsExactly("P-222BBB");
        assertThat(buses.existsBySucursalIdAndEstadoNot(xela, EstadoBus.INACTIVO)).isTrue();
    }

    @Test
    void bloquearUnaSucursalDentroDeUnaTransaccion() {
        UUID xela = insertarSucursal("XELA", true);

        Boolean encontrada = new TransactionTemplate(transacciones)
                .execute(estado -> sucursales.bloquearPorId(xela).isPresent());

        assertThat(encontrada).isTrue();
    }
}
