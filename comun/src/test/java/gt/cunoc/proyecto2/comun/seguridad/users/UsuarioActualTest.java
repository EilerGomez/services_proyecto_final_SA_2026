package gt.cunoc.proyecto2.comun.seguridad.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioActualTest {

    private final UUID sucursal = UUID.randomUUID();

    @Test
    void adminDeSucursalSoloAccedeASuSucursal() {
        UsuarioActual admin = new UsuarioActual(UUID.randomUUID(), "a@b.com",
                Set.of(Roles.ADMIN_SUCURSAL), sucursal, null);

        assertThat(admin.esAdminSistema()).isFalse();
        assertThatCode(() -> admin.verificarAccesoSucursal(sucursal)).doesNotThrowAnyException();
        assertThatThrownBy(() -> admin.verificarAccesoSucursal(UUID.randomUUID()))
                .isInstanceOf(ProhibidoException.class);
    }

    @Test
    void adminDeSistemaAccedeACualquierSucursal() {
        UsuarioActual admin = new UsuarioActual(UUID.randomUUID(), "a@b.com",
                Set.of(Roles.ADMIN_SISTEMA), null, null);

        assertThat(admin.esAdminSistema()).isTrue();
        assertThatCode(() -> admin.verificarAccesoSucursal(UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    void clienteSinSucursalNoAccedeANinguna() {
        UsuarioActual cliente = new UsuarioActual(UUID.randomUUID(), "c@b.com", Set.of(Roles.CLIENTE), null, null);

        assertThatThrownBy(() -> cliente.verificarAccesoSucursal(sucursal)).isInstanceOf(ProhibidoException.class);
    }

    @Test
    void consultaDeRoles() {
        UsuarioActual cajero = new UsuarioActual(UUID.randomUUID(), "c@b.com",
                Set.of(Roles.CAJERO, Roles.CLIENTE), sucursal, null);

        assertThat(cajero.tieneRol(Roles.CAJERO)).isTrue();
        assertThat(cajero.tieneRol(Roles.CHOFER)).isFalse();
        assertThat(cajero.tieneAlgunRol(Roles.CHOFER, Roles.CAJERO)).isTrue();
        assertThat(cajero.tieneAlgunRol(Roles.CHOFER, Roles.ADMIN_SISTEMA)).isFalse();
    }

    @Test
    void rolesNulosQuedanVacios() {
        UsuarioActual u = new UsuarioActual(UUID.randomUUID(), "x@y.com", null, null, null);
        assertThat(u.roles()).isEmpty();
    }
}
