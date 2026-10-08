package gt.cunoc.proyecto2.fleet.controladores.personal;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.dtos.personal.DesactivarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.NuevoPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladoPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.servicios.personal.PersonalSucursalServicio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PersonalSucursalControladorTest {

    private final PersonalSucursalServicio servicio = mock(PersonalSucursalServicio.class);
    private final PersonalSucursalControlador controlador = new PersonalSucursalControlador(servicio);
    private final UUID sucursalId = UUID.randomUUID();
    private final UsuarioActual admin = DatosPrueba.adminSucursal(sucursalId);
    private final PersonalSucursalEntidad cajero = DatosPrueba.personal(sucursalId, RolPersonal.CAJERO);

    @Test
    void listarConvierteLasEntidades() {
        when(servicio.listar(sucursalId, RolPersonal.CAJERO, true, admin)).thenReturn(List.of(cajero));

        var respuesta = controlador.listar(sucursalId, RolPersonal.CAJERO, true, admin);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).extracting(PersonalSucursalRespuesta::personalId)
                .containsExactly(cajero.getId());
    }

    @Test
    void agregarResponde201() {
        NuevoPersonalSolicitud solicitud = new NuevoPersonalSolicitud(RolPersonal.CAJERO, FECHA,
                DatosPrueba.cuenta(null, null));
        PersonalSucursalRespuesta creado = new PersonalSucursalRespuesta(cajero);
        when(servicio.agregar(sucursalId, solicitud, admin)).thenReturn(creado);

        var respuesta = controlador.agregar(sucursalId, solicitud, admin);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isSameAs(creado);
    }

    @Test
    void desactivarRespondeOk() {
        DesactivarPersonalSolicitud solicitud = new DesactivarPersonalSolicitud(FECHA, 0);
        PersonalSucursalRespuesta desactivado = new PersonalSucursalRespuesta(cajero);
        when(servicio.desactivar(cajero.getId(), solicitud, admin)).thenReturn(desactivado);

        assertThat(controlador.desactivar(cajero.getId(), solicitud, admin).getBody()).isSameAs(desactivado);
    }

    @Test
    void trasladarResponde201() {
        TrasladarPersonalSolicitud solicitud = new TrasladarPersonalSolicitud(UUID.randomUUID(), FECHA, 0);
        TrasladoPersonalRespuesta traslado = new TrasladoPersonalRespuesta(new PersonalSucursalRespuesta(cajero),
                new PersonalSucursalRespuesta(cajero));
        when(servicio.trasladar(cajero.getId(), solicitud)).thenReturn(traslado);

        var respuesta = controlador.trasladar(cajero.getId(), solicitud);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isSameAs(traslado);
    }
}
