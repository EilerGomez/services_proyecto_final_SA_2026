package gt.cunoc.proyecto2.fleet.dtos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitudCuentaPersonalTest {

    private final UUID sucursalId = UUID.randomUUID();
    private final DatosCuentaSolicitud datos = DatosPrueba.cuenta(null, DatosPrueba.FOTO_CHOFER);

    @Test
    void cuentaDePersonalSinChofer() {
        UUID personalId = UUID.randomUUID();

        SolicitudCuentaPersonal solicitud = SolicitudCuentaPersonal.paraPersonal(personalId, sucursalId,
                RolPersonal.CAJERO, datos);

        assertThat(solicitud.solicitudId()).isEqualTo(personalId);
        assertThat(solicitud.rol()).isEqualTo("CAJERO");
        assertThat(solicitud.sucursalId()).isEqualTo(sucursalId);
        assertThat(solicitud.choferId()).isNull();
        assertThat(solicitud.correo()).isEqualTo(datos.correo());
        assertThat(solicitud.dpi()).isEqualTo(datos.dpi());
    }

    @Test
    void cuentaDeChoferUsaElMismoIdComoSolicitudYChofer() {
        UUID choferId = UUID.randomUUID();

        SolicitudCuentaPersonal solicitud = SolicitudCuentaPersonal.paraChofer(choferId, sucursalId, datos);

        assertThat(solicitud.solicitudId()).isEqualTo(choferId);
        assertThat(solicitud.choferId()).isEqualTo(choferId);
        assertThat(solicitud.rol()).isEqualTo(Roles.CHOFER);
        assertThat(solicitud.fotoUrl()).isEqualTo(DatosPrueba.FOTO_CHOFER);
    }
}
