package gt.cunoc.proyecto2.identity.integracion.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Lo que Identity hace con los eventos de Fleet: ChoferCreado y PersonalSucursalAsignado ponen el rol
 * y la sucursal en la cuenta que ya existía (altas por R5b), y ChoferDesactivado y
 * PersonalSucursalDesactivado los quitan y cierran las sesiones abiertas.
 *
 * <p>Son la última defensa de las reglas de cuenta: entre la validación de R5b y la llegada del evento
 * la cuenta pudo cambiar, así que un administrador del sistema, un chofer o alguien que ya es personal
 * de sucursal no reciben el rol, y el evento se descarta sin romper la cola.
 */
class EventosFleetIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(10);
    private static final String FLEET = "fleet.eventos";
    private static final UUID SUCURSAL = UUID.randomUUID();
    private static final UUID OTRA_SUCURSAL = UUID.randomUUID();

    private void publicarFleet(String tipo, String payload) {
        publicarEvento(FLEET, tipo, UUID.randomUUID(), payload);
    }

    private void choferCreado(UUID usuarioId, UUID choferId, UUID sucursalId) {
        publicarFleet("ChoferCreado", """
                {"choferId":"%s","usuarioId":%s,"sucursalId":"%s"}
                """.formatted(choferId, usuarioId == null ? "null" : "\"" + usuarioId + "\"", sucursalId));
    }

    private void choferDesactivado(UUID usuarioId, UUID choferId, UUID sucursalId) {
        publicarFleet("ChoferDesactivado", """
                {"choferId":"%s","usuarioId":"%s","sucursalId":"%s"}
                """.formatted(choferId, usuarioId, sucursalId));
    }

    private void personalAsignado(UUID usuarioId, UUID sucursalId, String rolCodigo) {
        publicarFleet("PersonalSucursalAsignado", """
                {"personalId":"%s","usuarioId":"%s","sucursalId":"%s","rolCodigo":"%s",
                 "fechaAsignacion":"2026-10-20","sucursalNombre":"Xela Central"}
                """.formatted(UUID.randomUUID(), usuarioId, sucursalId, rolCodigo));
    }

    private void personalDesactivado(UUID usuarioId, UUID sucursalId, String rolCodigo) {
        publicarFleet("PersonalSucursalDesactivado", """
                {"personalId":"%s","usuarioId":"%s","sucursalId":"%s","rolCodigo":"%s",
                 "fechaDesasignacion":"2026-10-20"}
                """.formatted(UUID.randomUUID(), usuarioId, sucursalId, rolCodigo));
    }

    private List<String> rolesDe(UUID usuarioId) {
        return jdbc.queryForList("SELECT rol_codigo FROM usuario_roles WHERE usuario_id = ?",
                String.class, usuarioId);
    }

    private UUID columnaDe(String columna, UUID usuarioId) {
        return jdbc.queryForObject("SELECT " + columna + " FROM usuarios WHERE id = ?", UUID.class, usuarioId);
    }

    private int publicados(String tipoEvento) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM eventos_salida WHERE tipo_evento = ?",
                Integer.class, tipoEvento);
        return total == null ? 0 : total;
    }

    private void esperarProcesados(int cuantos) {
        await().atMost(ESPERA).until(() -> contar("eventos_procesados") == cuantos);
    }

    private UUID registrarAna() {
        return registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
    }

    // ----- ChoferCreado -----

    @Test
    void choferCreadoLeDaElRolLaSucursalYElChoferIdALaCuentaQueYaExistia() {
        UUID ana = registrarAna();
        UUID choferId = UUID.randomUUID();

        choferCreado(ana, choferId, SUCURSAL);
        esperarEventos("RolAsignado");

        assertThat(rolesDe(ana)).containsExactlyInAnyOrder(Roles.CLIENTE, Roles.CHOFER);
        assertThat(columnaDe("chofer_id", ana)).isEqualTo(choferId);
        assertThat(columnaDe("sucursal_id", ana)).isEqualTo(SUCURSAL);

        // El mismo evento otra vez (Fleet reintentó): ya tiene ese choferId, no se vuelve a avisar
        choferCreado(ana, choferId, SUCURSAL);
        esperarProcesados(2);
        assertThat(rolesDe(ana)).hasSize(2);
        assertThat(publicados("RolAsignado")).isEqualTo(1);
    }

    @Test
    void choferCreadoSinUsuarioOConUnoDesconocidoSeDescarta() {
        registrarAna();

        choferCreado(null, UUID.randomUUID(), SUCURSAL);
        choferCreado(UUID.randomUUID(), UUID.randomUUID(), SUCURSAL);

        esperarProcesados(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario_roles WHERE rol_codigo = ?",
                Integer.class, Roles.CHOFER)).isZero();
        assertThat(publicados("RolAsignado")).isZero();
    }

    @Test
    void niUnAdminDelSistemaNiAlguienQueYaEsPersonalRecibenElRolDeChofer() {
        UUID beto = registrarCliente("beto@pruebas.gt", "2000000-2", "2000000000102");
        jdbc.update("UPDATE usuarios SET sucursal_id = ? WHERE id = ?", SUCURSAL, beto);

        choferCreado(adminId, UUID.randomUUID(), SUCURSAL);     // ES_ADMIN_SISTEMA
        choferCreado(beto, UUID.randomUUID(), SUCURSAL);        // ES_PERSONAL_SUCURSAL

        esperarProcesados(2);
        assertThat(rolesDe(adminId)).doesNotContain(Roles.CHOFER);
        assertThat(rolesDe(beto)).doesNotContain(Roles.CHOFER);
        assertThat(columnaDe("chofer_id", adminId)).isNull();
        assertThat(columnaDe("chofer_id", beto)).isNull();
        assertThat(publicados("RolAsignado")).isZero();
    }

    // ----- ChoferDesactivado -----

    @Test
    void choferDesactivadoLeQuitaElRolLaSucursalYCierraSusSesiones() {
        UUID ana = registrarAna();
        UUID choferId = UUID.randomUUID();
        choferCreado(ana, choferId, SUCURSAL);
        esperarEventos("RolAsignado");
        String refresco = PruebaIntegracion.json(post("/identity/auth/login", null,
                "{\"correo\":\"ana@pruebas.gt\",\"contrasena\":\"%s\"}".formatted(CONTRASENA)), "$.tokenRefresco");

        choferDesactivado(ana, choferId, SUCURSAL);

        esperarEventos("SesionRevocada");
        assertThat(rolesDe(ana)).containsExactly(Roles.CLIENTE);
        assertThat(columnaDe("sucursal_id", ana)).isNull();
        // La sesión que ya tenía abierta queda revocada
        esError(post("/identity/auth/refrescar", null,
                "{\"tokenRefresco\":\"%s\"}".formatted(refresco)), 401, "TOKEN_REFRESCO_INVALIDO");
    }

    @Test
    void unChoferDesconocidoSeIgnoraYElDeOtraSucursalNoLeBorraLaSuya() {
        UUID ana = registrarAna();
        UUID choferId = UUID.randomUUID();
        choferCreado(ana, choferId, SUCURSAL);
        esperarEventos("RolAsignado");

        choferDesactivado(ana, UUID.randomUUID(), SUCURSAL);        // ningún usuario tiene ese choferId
        choferDesactivado(ana, choferId, OTRA_SUCURSAL);            // el retiro viene de otra sucursal

        esperarProcesados(3);
        // Le quitan el rol de chofer, pero la sucursal donde sigue asignado no se toca
        assertThat(rolesDe(ana)).containsExactly(Roles.CLIENTE);
        assertThat(columnaDe("sucursal_id", ana)).isEqualTo(SUCURSAL);
    }

    // ----- Personal de sucursal -----

    @Test
    void personalDesactivadoSoloLeQuitaElRolSiVieneDeSuSucursal() {
        UUID ana = registrarAna();
        personalAsignado(ana, SUCURSAL, Roles.CAJERO);
        esperarEventos("RolAsignado");

        personalDesactivado(UUID.randomUUID(), SUCURSAL, Roles.CAJERO);   // usuario desconocido
        personalDesactivado(ana, OTRA_SUCURSAL, Roles.CAJERO);            // otra sucursal: se ignora
        esperarProcesados(3);
        assertThat(rolesDe(ana)).containsExactlyInAnyOrder(Roles.CLIENTE, Roles.CAJERO);
        assertThat(columnaDe("sucursal_id", ana)).isEqualTo(SUCURSAL);

        personalDesactivado(ana, SUCURSAL, Roles.CAJERO);

        esperarEventos("SesionRevocada");
        assertThat(rolesDe(ana)).containsExactly(Roles.CLIENTE);
        assertThat(columnaDe("sucursal_id", ana)).isNull();
    }

    @Test
    void elRolQueLlegaConPersonalSucursalAsignadoTieneQueSerDeSucursalYNuevo() {
        UUID ana = registrarAna();

        // CHOFER no entra por aquí (llega con ChoferCreado) y un rol que no existe se descarta:
        // en los dos casos la sucursal se guarda pero no se avisa de ningún rol nuevo
        personalAsignado(ana, SUCURSAL, Roles.CHOFER);
        personalAsignado(ana, SUCURSAL, "NO_EXISTE");
        esperarProcesados(2);
        assertThat(rolesDe(ana)).containsExactly(Roles.CLIENTE);
        assertThat(columnaDe("sucursal_id", ana)).isEqualTo(SUCURSAL);
        assertThat(publicados("RolAsignado")).isZero();

        // El rol de sucursal sí, y repetirlo no vuelve a avisar
        personalAsignado(ana, SUCURSAL, Roles.ADMIN_SUCURSAL);
        esperarEventos("RolAsignado");
        personalAsignado(ana, SUCURSAL, Roles.ADMIN_SUCURSAL);
        esperarProcesados(4);
        assertThat(rolesDe(ana)).containsExactlyInAnyOrder(Roles.CLIENTE, Roles.ADMIN_SUCURSAL);
        assertThat(publicados("RolAsignado")).isEqualTo(1);

        // Y a un administrador del sistema no se le asigna personal
        personalAsignado(adminId, SUCURSAL, Roles.CAJERO);
        esperarProcesados(5);
        assertThat(rolesDe(adminId)).doesNotContain(Roles.CAJERO);
        assertThat(columnaDe("sucursal_id", adminId)).isNull();
    }
}
