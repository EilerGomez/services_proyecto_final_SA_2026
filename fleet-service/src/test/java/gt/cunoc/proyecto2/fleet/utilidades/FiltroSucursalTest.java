package gt.cunoc.proyecto2.fleet.utilidades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FiltroSucursalTest {

    private final UUID xela = UUID.randomUUID();

    @Test
    void adminSistemaFiltraPorLaQuePideOVeTodas() {
        UsuarioActual admin = DatosPrueba.adminSistema();

        assertThat(FiltroSucursal.resolver(admin, xela)).isEqualTo(xela);
        assertThat(FiltroSucursal.resolver(admin, null)).isNull();
    }

    @Test
    void adminSucursalSiempreUsaLaDeSuToken() {
        UsuarioActual admin = DatosPrueba.adminSucursal(xela);

        assertThat(FiltroSucursal.resolver(admin, null)).isEqualTo(xela);
        assertThat(FiltroSucursal.resolver(admin, xela)).isEqualTo(xela);
    }
}
