package gt.cunoc.proyecto2.identity.services.personal;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasAsignacionRolTest {

    private static final Instant AHORA = Instant.parse("2026-10-20T12:00:00Z");

    @Test
    void clienteActivoPuedeSerPersonalYAdminSistema() {
        Usuario cliente = usuario("ACTIVO", Roles.CLIENTE);

        assertThat(ReglasAsignacionRol.impedimentoParaPersonal(cliente, AHORA)).isEmpty();
        assertThat(ReglasAsignacionRol.impedimentoParaAdminSistema(cliente, AHORA)).isEmpty();
    }

    @Test
    void bloqueoVencidoNoImpide() {
        Usuario usuario = usuario("BLOQUEADO", Roles.CLIENTE);
        usuario.setBloqueadoHasta(AHORA.minusSeconds(60));

        assertThat(ReglasAsignacionRol.impedimentoParaPersonal(usuario, AHORA)).isEmpty();
    }

    @Test
    void bloqueoVigenteOIndefinidoImpide() {
        Usuario vigente = usuario("BLOQUEADO", Roles.CLIENTE);
        vigente.setBloqueadoHasta(AHORA.plusSeconds(60));
        Usuario indefinido = usuario("BLOQUEADO", Roles.CLIENTE);

        assertThat(codigo(ReglasAsignacionRol.impedimentoParaPersonal(vigente, AHORA))).isEqualTo("CUENTA_BLOQUEADA");
        assertThat(codigo(ReglasAsignacionRol.impedimentoParaAdminSistema(indefinido, AHORA)))
                .isEqualTo("CUENTA_BLOQUEADA");
    }

    @Test
    void cuentaInactivaImpide() {
        Usuario inactivo = usuario("INACTIVO", Roles.CLIENTE);

        assertThat(codigo(ReglasAsignacionRol.impedimentoParaPersonal(inactivo, AHORA))).isEqualTo("CUENTA_INACTIVA");
        assertThat(codigo(ReglasAsignacionRol.impedimentoParaAdminSistema(inactivo, AHORA)))
                .isEqualTo("CUENTA_INACTIVA");
    }

    @Test
    void adminSistemaNoPuedeSerPersonalNiPromoverseOtraVez() {
        Usuario admin = usuario("ACTIVO", Roles.ADMIN_SISTEMA, Roles.CLIENTE);

        Optional<NegocioException> personal = ReglasAsignacionRol.impedimentoParaPersonal(admin, AHORA);
        assertThat(codigo(personal)).isEqualTo("ES_ADMIN_SISTEMA");
        assertThat(personal.orElseThrow().getStatus().value()).isEqualTo(422);
        Optional<NegocioException> otraVez = ReglasAsignacionRol.impedimentoParaAdminSistema(admin, AHORA);
        assertThat(codigo(otraVez)).isEqualTo("YA_ES_ADMIN_SISTEMA");
        assertThat(otraVez.orElseThrow().getStatus().value()).isEqualTo(409);
    }

    @Test
    void choferOExChoferNoPuedeRecibirRolesAdministrativos() {
        Usuario chofer = usuario("ACTIVO", Roles.CLIENTE);
        chofer.setChoferId(UUID.randomUUID());

        assertThat(codigo(ReglasAsignacionRol.impedimentoParaPersonal(chofer, AHORA))).isEqualTo("ES_CHOFER");
        assertThat(codigo(ReglasAsignacionRol.impedimentoParaAdminSistema(chofer, AHORA))).isEqualTo("ES_CHOFER");
    }

    @ParameterizedTest
    @ValueSource(strings = {Roles.ADMIN_SUCURSAL, Roles.CAJERO, "SOLO_SUCURSAL"})
    void personalDeSucursalNoPuedeAsignarseOtraVezNiSerAdminSistema(String caso) {
        Usuario usuario = "SOLO_SUCURSAL".equals(caso)
                ? usuario("ACTIVO", Roles.CLIENTE)
                : usuario("ACTIVO", caso, Roles.CLIENTE);
        if ("SOLO_SUCURSAL".equals(caso)) {
            usuario.setSucursalId(UUID.randomUUID());
        }

        Optional<NegocioException> personal = ReglasAsignacionRol.impedimentoParaPersonal(usuario, AHORA);
        assertThat(codigo(personal)).isEqualTo("YA_ES_PERSONAL");
        assertThat(personal.orElseThrow().getStatus().value()).isEqualTo(409);
        assertThat(codigo(ReglasAsignacionRol.impedimentoParaAdminSistema(usuario, AHORA)))
                .isEqualTo("ES_PERSONAL_SUCURSAL");
    }

    private static String codigo(Optional<NegocioException> impedimento) {
        return impedimento.map(NegocioException::getCodigo).orElse(null);
    }

    static Usuario usuario(String estado, String... roles) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreo("ana@correo.com");
        usuario.setEstado(estado);
        for (String codigo : roles) {
            usuario.getRoles().add(rol(codigo));
        }
        return usuario;
    }

    static Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        return rol;
    }
}
