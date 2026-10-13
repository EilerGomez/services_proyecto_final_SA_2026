package gt.cunoc.proyecto2.maintenance.servicios.taller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.LineaServicioSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.NuevoServicioSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.ServicioRespuesta;
import gt.cunoc.proyecto2.maintenance.eventos.PublicadorEventosMaintenance;
import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.DetalleServicioEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import gt.cunoc.proyecto2.maintenance.repositorios.CategoriaRepositorio;
import gt.cunoc.proyecto2.maintenance.repositorios.ServicioRepositorio;
import gt.cunoc.proyecto2.maintenance.repositorios.VistaBusRepositorio;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicioTallerServicioTest {

    @Mock
    ServicioRepositorio servicios;
    @Mock
    CategoriaRepositorio categorias;
    @Mock
    VistaBusRepositorio buses;
    @Mock
    PublicadorEventosMaintenance eventos;
    @Mock
    EntityManager entityManager;

    @InjectMocks
    ServicioTallerServicioImpl servicio;

    private final UUID sucursalId = UUID.randomUUID();
    private final UUID busId = UUID.randomUUID();

    private UsuarioActual adminSucursal() {
        return new UsuarioActual(UUID.randomUUID(), "a@m.gt", Set.of(Roles.ADMIN_SUCURSAL),
                sucursalId, null);
    }

    private UsuarioActual adminSistema() {
        return new UsuarioActual(UUID.randomUUID(), "s@m.gt", Set.of(Roles.ADMIN_SISTEMA),
                null, null);
    }

    private VistaBusEntidad bus(boolean activo) {
        VistaBusEntidad bus = new VistaBusEntidad();
        bus.setBusId(busId);
        bus.setPlaca("C123ABC");
        bus.setSucursalId(sucursalId);
        bus.setActivo(activo);
        return bus;
    }

    private NuevoServicioSolicitud solicitud(UUID categoriaId) {
        return new NuevoServicioSolicitud(busId, LocalDate.of(2026, 10, 22), "Taller", "Obs",
                List.of(new LineaServicioSolicitud(categoriaId, "4 llantas", "REPUESTO",
                        new BigDecimal("6800.00"))));
    }

    @Test
    void registrarGuardaPublicaYResponde() {
        UUID categoriaId = UUID.randomUUID();
        var usuario = adminSucursal();
        when(buses.findById(busId)).thenReturn(Optional.of(bus(true)));
        CategoriaEntidad categoria = new CategoriaEntidad();
        categoria.setId(categoriaId);
        categoria.setNombre("Llantas");
        when(categorias.findById(categoriaId)).thenReturn(Optional.of(categoria));

        var respuesta = servicio.registrar(solicitud(categoriaId), usuario);

        assertThat(respuesta.busId()).isEqualTo(busId);
        assertThat(respuesta.placa()).isEqualTo("C123ABC");
        assertThat(respuesta.lineas()).hasSize(1);
        assertThat(respuesta.lineas().get(0).categoriaNombre()).isEqualTo("Llantas");
        verify(servicios).saveAndFlush(any(ServicioTallerEntidad.class));
        verify(entityManager).refresh(any(ServicioTallerEntidad.class));
        verify(eventos).gastoRegistrado(any(ServicioTallerEntidad.class));
        ArgumentCaptor<ServicioTallerEntidad> captor =
                ArgumentCaptor.forClass(ServicioTallerEntidad.class);
        verify(servicios).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSucursalId()).isEqualTo(sucursalId);
        assertThat(captor.getValue().getRegistradoPor()).isEqualTo(usuario.usuarioId());
    }

    @Test
    void registrarBusInexistenteEs404() {
        when(buses.findById(busId)).thenReturn(Optional.empty());

        var lanzada = catchThrowable(
                () -> servicio.registrar(solicitud(UUID.randomUUID()), adminSucursal()));
        assertThat(lanzada).isInstanceOf(NoEncontradoException.class);
        assertThat(((gt.cunoc.proyecto2.comun.errores.NegocioException) lanzada).getCodigo())
                .isEqualTo("BUS_NO_EXISTE");
        verify(servicios, never()).saveAndFlush(any());
    }

    @Test
    void registrarBusDeOtraSucursalEs403() {
        VistaBusEntidad ajeno = bus(true);
        ajeno.setSucursalId(UUID.randomUUID());
        when(buses.findById(busId)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> servicio.registrar(solicitud(UUID.randomUUID()), adminSucursal()))
                .isInstanceOf(ProhibidoException.class);
    }

    @Test
    void registrarBusInactivoEs422() {
        when(buses.findById(busId)).thenReturn(Optional.of(bus(false)));

        var lanzada = catchThrowable(
                () -> servicio.registrar(solicitud(UUID.randomUUID()), adminSucursal()));
        assertThat(lanzada).isInstanceOf(ReglaNegocioException.class);
        assertThat(((gt.cunoc.proyecto2.comun.errores.NegocioException) lanzada).getCodigo())
                .isEqualTo("BUS_INACTIVO");
    }

    @Test
    void registrarCategoriaInexistenteEs404() {
        UUID categoriaId = UUID.randomUUID();
        when(buses.findById(busId)).thenReturn(Optional.of(bus(true)));
        when(categorias.findById(categoriaId)).thenReturn(Optional.empty());

        var lanzada = catchThrowable(
                () -> servicio.registrar(solicitud(categoriaId), adminSucursal()));
        assertThat(lanzada).isInstanceOf(NoEncontradoException.class);
        assertThat(((gt.cunoc.proyecto2.comun.errores.NegocioException) lanzada).getCodigo())
                .isEqualTo("CATEGORIA_NO_EXISTE");
    }

    private ServicioTallerEntidad servicioDelBus(UUID busDelServicio) {
        ServicioTallerEntidad entidad = new ServicioTallerEntidad();
        entidad.setId(UUID.randomUUID());
        entidad.setBusId(busDelServicio);
        entidad.setSucursalId(sucursalId);
        entidad.setFechaServicio(LocalDate.of(2026, 10, 22));
        return entidad;
    }

    @Test
    void listarFiltraPorSucursalDelToken() {
        when(servicios.buscar(any(), any(), any(), any(), any())).thenReturn(List.of(servicioDelBus(busId)));
        when(buses.findAllById(any())).thenReturn(List.of(bus(true)));

        var lista = servicio.listar(null, null, null, null, adminSucursal());

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).lineas()).isNull();
        assertThat(lista.get(0).placa()).isEqualTo("C123ABC");
        var captor = ArgumentCaptor.forClass(UUID.class);
        verify(servicios).buscar(captor.capture(), any(), any(), any(), any());
        assertThat(captor.getValue()).isEqualTo(sucursalId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarBuscaSoloLasPlacasDeLosBusesDelResultado() {
        UUID sinVista = UUID.randomUUID();
        when(servicios.buscar(any(), any(), any(), any(), any())).thenReturn(
                List.of(servicioDelBus(busId), servicioDelBus(busId), servicioDelBus(sinVista)));
        when(buses.findAllById(any())).thenReturn(List.of(bus(true)));

        var lista = servicio.listar(null, null, null, null, adminSucursal());

        assertThat(lista).extracting(ServicioRespuesta::placa).containsExactly("C123ABC", "C123ABC", "-");
        ArgumentCaptor<Iterable<UUID>> ids = ArgumentCaptor.forClass(Iterable.class);
        verify(buses).findAllById(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(busId, sinVista);
        verify(buses, never()).findAll();
    }

    @Test
    void listarAdminSistemaVeTodo() {
        when(servicios.buscar(any(), any(), any(), any(), any())).thenReturn(List.of());

        assertThat(servicio.listar(null, null, null, null, adminSistema())).isEmpty();
        var captor = ArgumentCaptor.forClass(UUID.class);
        verify(servicios).buscar(captor.capture(), any(), any(), any(), any());
        assertThat(captor.getValue()).isNull();
    }

    @Test
    void listarSinSucursalEnElTokenEs403() {
        var sinSucursal = new UsuarioActual(UUID.randomUUID(), "a@m.gt", Set.of(Roles.ADMIN_SUCURSAL),
                null, null);

        var lanzada = catchThrowable(() -> servicio.listar(null, null, null, null, sinSucursal));
        assertThat(lanzada).isInstanceOf(ProhibidoException.class);
        assertThat(((ProhibidoException) lanzada).getCodigo()).isEqualTo("SUCURSAL_AJENA");
        verify(servicios, never()).buscar(any(), any(), any(), any(), any());
    }

    @Test
    void obtenerConLineas() {
        UUID id = UUID.randomUUID();
        ServicioTallerEntidad entidad = new ServicioTallerEntidad();
        entidad.setId(id);
        entidad.setBusId(busId);
        entidad.setSucursalId(sucursalId);
        entidad.setFechaServicio(LocalDate.of(2026, 10, 22));
        DetalleServicioEntidad linea = new DetalleServicioEntidad();
        linea.setId(UUID.randomUUID());
        linea.setCategoriaId(UUID.randomUUID());
        linea.setDescripcion("Aceite");
        linea.setTipo("MANO_OBRA");
        linea.setMonto(new BigDecimal("350"));
        entidad.agregarLinea(linea);
        when(servicios.findById(id)).thenReturn(Optional.of(entidad));
        when(buses.findById(busId)).thenReturn(Optional.of(bus(true)));

        var respuesta = servicio.obtener(id, adminSucursal());

        assertThat(respuesta.lineas()).hasSize(1);
        assertThat(respuesta.lineas().get(0).categoriaNombre()).isNull();
    }

    @Test
    void obtenerInexistenteEs404() {
        UUID id = UUID.randomUUID();
        when(servicios.findById(id)).thenReturn(Optional.empty());

        var lanzada = catchThrowable(() -> servicio.obtener(id, adminSistema()));
        assertThat(lanzada).isInstanceOf(NoEncontradoException.class);
        assertThat(((gt.cunoc.proyecto2.comun.errores.NegocioException) lanzada).getCodigo())
                .isEqualTo("SERVICIO_NO_EXISTE");
    }

    @Test
    void obtenerDeOtraSucursalEs403() {
        UUID id = UUID.randomUUID();
        ServicioTallerEntidad entidad = new ServicioTallerEntidad();
        entidad.setId(id);
        entidad.setSucursalId(UUID.randomUUID());
        when(servicios.findById(id)).thenReturn(Optional.of(entidad));

        assertThatThrownBy(() -> servicio.obtener(id, adminSucursal()))
                .isInstanceOf(ProhibidoException.class);
    }
}
