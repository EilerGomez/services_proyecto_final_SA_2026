package gt.cunoc.proyecto2.notification.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.notification.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordatoriosLicenciaIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);

    private static String licencia(UUID choferId, UUID usuarioId, UUID sucursalId, String vence) {
        return """
                {"choferId":"%s","usuarioId":"%s","sucursalId":"%s","fechaVencimientoLicencia":"%s"}
                """.formatted(choferId, usuarioId, sucursalId, vence);
    }

    private List<Map<String, Object>> recordatorios(UUID choferId) {
        return jdbc.queryForList("""
                SELECT dias_antes, fecha_programada::text AS programada, fecha_vencimiento::text AS vence, estado
                FROM recordatorios_licencia WHERE chofer_id = ? ORDER BY fecha_vencimiento, dias_antes DESC""",
                choferId);
    }

    @Test
    void programaLosTresRecordatoriosSinEnviarCorreos() {
        UUID chofer = UUID.randomUUID();

        UUID eventId = publicarEvento(FLEET_EVENTOS, "LicenciaChoferActualizada", "2026-10-01",
                licencia(chofer, UUID.randomUUID(), UUID.randomUUID(), "2027-03-15"));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        List<Map<String, Object>> filas = recordatorios(chofer);
        assertThat(filas).extracting(f -> f.get("dias_antes")).containsExactly(30, 15, 3);
        assertThat(filas).extracting(f -> f.get("programada"))
                .containsExactly("2027-02-13", "2027-02-28", "2027-03-12");
        assertThat(filas).extracting(f -> f.get("estado")).containsOnly("PENDIENTE");
        assertThat(totalNotificaciones()).isZero();
        assertThat(totalCorreos()).isZero();
    }

    @Test
    void losQueYaPasaronNoSeCrean() {
        UUID chofer = UUID.randomUUID();

        // Registrada el 5-mar y vence el 15-mar: solo queda el aviso de 3 días antes (12-mar)
        UUID eventId = publicarEvento(FLEET_EVENTOS, "LicenciaChoferActualizada", "2027-03-05",
                licencia(chofer, UUID.randomUUID(), UUID.randomUUID(), "2027-03-15"));

        await().atMost(ESPERA).until(() -> procesado(eventId));
        assertThat(recordatorios(chofer)).extracting(f -> f.get("dias_antes")).containsExactly(3);
    }

    @Test
    void renovarLaLicenciaCancelaLosPendientesYProgramaLosNuevos() {
        UUID chofer = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID sucursal = UUID.randomUUID();
        UUID primera = publicarEvento(FLEET_EVENTOS, "LicenciaChoferActualizada", "2026-10-01",
                licencia(chofer, usuario, sucursal, "2027-03-15"));
        await().atMost(ESPERA).until(() -> procesado(primera));

        UUID renovacion = publicarEvento(FLEET_EVENTOS, "LicenciaChoferActualizada", "2026-10-10",
                licencia(chofer, usuario, sucursal, "2031-03-15"));

        await().atMost(ESPERA).until(() -> procesado(renovacion));
        List<Map<String, Object>> filas = recordatorios(chofer);
        assertThat(filas).hasSize(6);
        assertThat(filas.subList(0, 3)).extracting(f -> f.get("estado")).containsOnly("CANCELADO");
        assertThat(filas.subList(3, 6)).extracting(f -> f.get("vence")).containsOnly("2031-03-15");
        assertThat(filas.subList(3, 6)).extracting(f -> f.get("estado")).containsOnly("PENDIENTE");
    }

    @Test
    void volverALaFechaAnteriorReactivaLosRecordatorios() {
        UUID chofer = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID sucursal = UUID.randomUUID();
        UUID[] eventos = {
                publicarEvento(FLEET_EVENTOS, "LicenciaChoferActualizada", "2026-10-01",
                        licencia(chofer, usuario, sucursal, "2027-03-15")),
                publicarEvento(FLEET_EVENTOS, "LicenciaChoferActualizada", "2026-10-02",
                        licencia(chofer, usuario, sucursal, "2028-03-15")),
                publicarEvento(FLEET_EVENTOS, "LicenciaChoferActualizada", "2026-10-03",
                        licencia(chofer, usuario, sucursal, "2027-03-15"))};

        await().atMost(ESPERA).until(() -> procesado(eventos[2]));
        List<Map<String, Object>> filas = recordatorios(chofer);
        assertThat(filas).hasSize(6);                                   // no se duplican los de 2027
        assertThat(filas.subList(0, 3)).extracting(f -> f.get("estado")).containsOnly("PENDIENTE");
        assertThat(filas.subList(3, 6)).extracting(f -> f.get("estado")).containsOnly("CANCELADO");
    }
}