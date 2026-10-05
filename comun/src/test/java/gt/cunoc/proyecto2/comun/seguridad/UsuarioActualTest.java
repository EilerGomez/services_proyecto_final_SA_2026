package gt.cunoc.proyecto2.comun.seguridad;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
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

        assertThatCode(() -> admin.verificarAccesoSucursal(sucursal)).doesNotThrowAnyException();
        assertThatThrownBy(() -> admin.verificarAccesoSucursal(UUID.randomUUID()))
                .isInstanceOf(ProhibidoException.class);
    }

    @Test
    void adminDeSistemaAccedeACualquierSucursal() {
        UsuarioActual admin = new UsuarioActual(UUID.randomUUID(), "a@b.com",
                Set.of(Roles.ADMIN_SISTEMA), null, null);

        assertThatCode(() -> admin.verificarAccesoSucursal(UUID.randomUUID())).doesNotThrowAnyException();
    }
}
