package gt.cunoc.proyecto2.fleet.controladores.choferes;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarLicenciaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferInternoRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.DesactivarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.NuevoChoferSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import gt.cunoc.proyecto2.fleet.modelos.choferes.TipoLicencia;
import gt.cunoc.proyecto2.fleet.servicios.choferes.ChoferServicio;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ChoferControladorTest {

    private final ChoferServicio servicio = mock(ChoferServicio.class);
    private final ChoferControlador controlador = new ChoferControlador(servicio);
    private final UUID sucursalId = UUID.randomUUID();
    private final UsuarioActual admin = DatosPrueba.adminSucursal(sucursalId);
    private final ChoferEntidad chofer = DatosPrueba.chofer(sucursalId);

    @Test
    void consultasRespondenOk() {
        UsuarioActual usuarioChofer = DatosPrueba.usuarioChofer(chofer.getId());
        when(servicio.listar(sucursalId, EstadoChofer.ACTIVO, admin)).thenReturn(List.of(chofer));
        when(servicio.obtener(chofer.getId(), admin)).thenReturn(chofer);
        when(servicio.obtenerPropio(usuarioChofer)).thenReturn(chofer);

        assertThat(controlador.listar(sucursalId, EstadoChofer.ACTIVO, admin).getBody())
                .extracting(ChoferRespuesta::choferId).containsExactly(chofer.getId());
        assertThat(controlador.obtener(chofer.getId(), admin).getBody().nombreCompleto()).isEqualTo("Juan Perez");
        assertThat(controlador.obtenerPropio(usuarioChofer).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void crearResponde201() {
        NuevoChoferSolicitud solicitud = new NuevoChoferSolicitud(sucursalId, "LIC-1234", TipoLicencia.A,
                FECHA.plusYears(1), BigDecimal.TEN, FECHA, DatosPrueba.cuenta(null, DatosPrueba.FOTO_CHOFER));
        ChoferRespuesta creado = new ChoferRespuesta(chofer);
        when(servicio.crear(solicitud, admin)).thenReturn(creado);

        var respuesta = controlador.crear(solicitud, admin);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isSameAs(creado);
    }

    @Test
    void modificacionesRespondenOk() {
        ChoferRespuesta respuesta = new ChoferRespuesta(chofer);
        ActualizarChoferSolicitud salario = new ActualizarChoferSolicitud(BigDecimal.TEN, 0);
        ActualizarLicenciaSolicitud licencia = new ActualizarLicenciaSolicitud("LIC-1234", TipoLicencia.A,
                FECHA.plusYears(2), FECHA, 0);
        DesactivarChoferSolicitud desactivar = new DesactivarChoferSolicitud(FECHA, 0);
        when(servicio.actualizar(chofer.getId(), salario, admin)).thenReturn(respuesta);
        when(servicio.actualizarLicencia(chofer.getId(), licencia, admin)).thenReturn(respuesta);
        when(servicio.desactivar(chofer.getId(), desactivar, admin)).thenReturn(respuesta);

        assertThat(controlador.actualizar(chofer.getId(), salario, admin).getBody()).isSameAs(respuesta);
        assertThat(controlador.actualizarLicencia(chofer.getId(), licencia, admin).getBody()).isSameAs(respuesta);
        assertThat(controlador.desactivar(chofer.getId(), desactivar, admin).getBody()).isSameAs(respuesta);
    }

    @Test
    void consultaInternaParaTrips() {
        ChoferInternoControlador interno = new ChoferInternoControlador(servicio);
        ChoferInternoRespuesta datos = new ChoferInternoRespuesta(chofer, FECHA);
        when(servicio.consultarParaViaje(chofer.getId(), FECHA)).thenReturn(datos);

        assertThat(interno.consultar(chofer.getId(), FECHA).getBody()).isSameAs(datos);
    }
}
