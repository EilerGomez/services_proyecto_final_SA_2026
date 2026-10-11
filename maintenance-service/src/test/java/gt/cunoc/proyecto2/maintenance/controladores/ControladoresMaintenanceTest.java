package gt.cunoc.proyecto2.maintenance.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.maintenance.controladores.buses.BusControlador;
import gt.cunoc.proyecto2.maintenance.controladores.categorias.CategoriaControlador;
import gt.cunoc.proyecto2.maintenance.controladores.servicios.ServicioControlador;
import gt.cunoc.proyecto2.maintenance.dtos.buses.BusDisponibleRespuesta;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.ActualizarCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.CategoriaRespuesta;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.NuevaCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.NuevoServicioSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.ServicioRespuesta;
import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import gt.cunoc.proyecto2.maintenance.repositorios.VistaBusRepositorio;
import gt.cunoc.proyecto2.maintenance.servicios.categorias.CategoriaServicio;
import gt.cunoc.proyecto2.maintenance.servicios.taller.ServicioTallerServicio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControladoresMaintenanceTest {

    @Mock
    CategoriaServicio categorias;
    @Mock
    ServicioTallerServicio taller;
    @Mock
    VistaBusRepositorio buses;

    private UsuarioActual adminSucursal(UUID sucursalId) {
        return new UsuarioActual(UUID.randomUUID(), "a@m.gt", Set.of(Roles.ADMIN_SUCURSAL),
                sucursalId, null);
    }

    private CategoriaRespuesta categoria() {
        return new CategoriaRespuesta(UUID.randomUUID(), "Llantas", null, true);
    }

    private ServicioRespuesta servicio() {
        return new ServicioRespuesta(UUID.randomUUID(), UUID.randomUUID(), "C1", UUID.randomUUID(),
                null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                UUID.randomUUID(), null);
    }

    @Test
    void categoriasM1M2M3() {
        var controlador = new CategoriaControlador(categorias);
        when(categorias.listar(any())).thenReturn(List.of(categoria()));
        when(categorias.crear(any())).thenReturn(categoria());
        when(categorias.actualizar(any(), any())).thenReturn(categoria());

        assertThat(controlador.listar(null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controlador.listar(true).getBody()).hasSize(1);
        assertThat(controlador.crear(new NuevaCategoriaSolicitud("X", null)).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(controlador.actualizar(UUID.randomUUID(),
                new ActualizarCategoriaSolicitud("Y", null, true)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void busesM4FiltraPorSucursalYActivo() {
        var controlador = new BusControlador(buses);
        UUID sucursalId = UUID.randomUUID();
        VistaBusEntidad bus = new VistaBusEntidad();
        bus.setBusId(UUID.randomUUID());
        bus.setPlaca("C1");
        bus.setSucursalId(sucursalId);
        bus.setActivo(true);
        VistaBusEntidad inactivo = new VistaBusEntidad();
        inactivo.setBusId(UUID.randomUUID());
        inactivo.setPlaca("C2");
        inactivo.setSucursalId(sucursalId);
        inactivo.setActivo(false);
        when(buses.findBySucursalIdOrderByPlacaAsc(sucursalId))
                .thenReturn(List.of(bus, inactivo));

        var usuario = adminSucursal(sucursalId);
        List<BusDisponibleRespuesta> todos = controlador.listar(null, usuario).getBody();
        assertThat(todos).hasSize(2);
        List<BusDisponibleRespuesta> activos = controlador.listar(true, usuario).getBody();
        assertThat(activos).hasSize(1);
        assertThat(activos.get(0).placa()).isEqualTo("C1");
    }

    @Test
    void serviciosM5M6M7() {
        var controlador = new ServicioControlador(taller);
        when(taller.registrar(any(), any())).thenReturn(servicio());
        when(taller.listar(any(), any(), any(), any(), any())).thenReturn(List.of(servicio()));
        when(taller.obtener(any(), any())).thenReturn(servicio());
        var usuario = adminSucursal(UUID.randomUUID());

        assertThat(controlador.registrar(
                new NuevoServicioSolicitud(UUID.randomUUID(), null, null, null, List.of()),
                usuario).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controlador.listar(null, null, null, null, usuario).getBody()).hasSize(1);
        assertThat(controlador.obtener(UUID.randomUUID(), usuario).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void openApiExponeEsquemaJwt() {
        var config = new gt.cunoc.proyecto2.maintenance.configuracion.ConfiguracionOpenApi();
        assertThat(gt.cunoc.proyecto2.maintenance.configuracion.ConfiguracionOpenApi.ESQUEMA_JWT)
                .isEqualTo("jwt");
        assertThat(config).isNotNull();
    }
}
