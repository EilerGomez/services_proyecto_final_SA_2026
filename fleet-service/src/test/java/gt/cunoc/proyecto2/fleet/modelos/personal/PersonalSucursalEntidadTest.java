package gt.cunoc.proyecto2.fleet.modelos.personal;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PersonalSucursalEntidadTest {

    private final UUID xela = UUID.randomUUID();

    @Test
    void unaAsignacionNuevaQuedaActiva() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela, RolPersonal.CAJERO);
        PersonalSucursalEntidad admin = DatosPrueba.personal(xela, RolPersonal.ADMIN_SUCURSAL);

        assertThat(cajero.isActivo()).isTrue();
        assertThat(cajero.getFechaDesasignacion()).isNull();
        assertThat(cajero.esAdminSucursal()).isFalse();
        assertThat(admin.esAdminSucursal()).isTrue();
    }

    @Test
    void desactivarGuardaLaFecha() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela, RolPersonal.CAJERO);

        cajero.desactivar(FECHA);

        assertThat(cajero.isActivo()).isFalse();
        assertThat(cajero.getFechaDesasignacion()).isEqualTo(FECHA);
    }

    @Test
    void noSeDesactivaAntesDeLaAsignacion() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela, RolPersonal.CAJERO);
        var antes = cajero.getFechaAsignacion().minusDays(1);

        VerificarError.lanza(() -> cajero.desactivar(antes), ErrorPersonal.FECHA_DESASIGNACION_INVALIDA);
        assertThatThrownBy(() -> cajero.desactivar(antes))
                .hasMessageContaining(cajero.getFechaAsignacion().toString());
        assertThat(cajero.isActivo()).isTrue();
    }

    @Test
    void elTrasladoCierraLaAsignacionYCreaOtraEnElDestino() {
        PersonalSucursalEntidad anterior = DatosPrueba.personal(xela, RolPersonal.ADMIN_SUCURSAL);
        UUID guate = UUID.randomUUID();

        PersonalSucursalEntidad nueva = anterior.trasladarA(guate, FECHA);

        assertThat(anterior.isActivo()).isFalse();
        assertThat(anterior.getFechaDesasignacion()).isEqualTo(FECHA);
        assertThat(nueva.getId()).isNotEqualTo(anterior.getId());
        assertThat(nueva.getSucursalId()).isEqualTo(guate);
        assertThat(nueva.getUsuarioId()).isEqualTo(anterior.getUsuarioId());
        assertThat(nueva.getRol()).isEqualTo(RolPersonal.ADMIN_SUCURSAL);
        assertThat(nueva.getNombreCompleto()).isEqualTo(anterior.getNombreCompleto());
        assertThat(nueva.getCorreo()).isEqualTo(anterior.getCorreo());
        assertThat(nueva.isActivo()).isTrue();
        assertThat(nueva.getFechaAsignacion()).isEqualTo(FECHA);
        assertThat(nueva.getVersion()).isNull();
    }

    @Test
    void noSeReintegraUnaAsignacionActiva() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela, RolPersonal.CAJERO);

        VerificarError.lanza(() -> cajero.reintegrar(xela, RolPersonal.CAJERO, FECHA), ErrorPersonal.PERSONAL_ACTIVO);
    }

    @Test
    void elReintegroNoPuedeSerAntesDelRetiro() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela, RolPersonal.CAJERO);
        cajero.desactivar(FECHA);

        VerificarError.lanza(() -> cajero.reintegrar(xela, RolPersonal.CAJERO, FECHA.minusDays(1)),
                ErrorPersonal.FECHA_REINTEGRO_INVALIDA);
    }

    @Test
    void elReintegroCreaOtraAsignacionConLaMismaCuenta() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela, RolPersonal.CAJERO);
        cajero.desactivar(FECHA);
        UUID guate = UUID.randomUUID();

        PersonalSucursalEntidad nueva = cajero.reintegrar(guate, RolPersonal.ADMIN_SUCURSAL, FECHA);

        assertThat(nueva.getId()).isNotEqualTo(cajero.getId());
        assertThat(nueva.getUsuarioId()).isEqualTo(cajero.getUsuarioId());
        assertThat(nueva.getSucursalId()).isEqualTo(guate);
        assertThat(nueva.getRol()).isEqualTo(RolPersonal.ADMIN_SUCURSAL);
        assertThat(nueva.isActivo()).isTrue();
        assertThat(nueva.getFechaAsignacion()).isEqualTo(FECHA);
        assertThat(cajero.isActivo()).isFalse();
    }
}
