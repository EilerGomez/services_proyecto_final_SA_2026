package gt.cunoc.proyecto2.trips.servicios.rutas;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.dtos.rutas.ActualizarRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.NuevaRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.RutaRespuesta;
import gt.cunoc.proyecto2.trips.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.trips.errores.rutas.ErrorRuta;
import gt.cunoc.proyecto2.trips.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.trips.eventos.PublicadorEventosTrips;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.repositorios.rutas.RutaRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.servicios.sucursales.VistaSucursalServicio;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class RutaServicioImplTest {

    private static final UUID ANTIGUA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    /** Sucursal que no está en vista_sucursales: su nombre sale null. */
    private static final UUID SIN_NOMBRE = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final Map<UUID, String> NOMBRES = Map.of(XELA, "Xela", GUATE, "Guate", ANTIGUA, "Antigua");

    private final RutaRepositorio repositorio = mock(RutaRepositorio.class);
    private final VistaSucursalServicio sucursales = mock(VistaSucursalServicio.class);
    private final ViajeRepositorio viajes = mock(ViajeRepositorio.class);
    private final PublicadorEventosTrips eventos = mock(PublicadorEventosTrips.class);

    private RutaServicioImpl servicio;
    private RutaEntidad ruta;

    @BeforeEach
    void preparar() {
        servicio = new RutaServicioImpl(repositorio, sucursales, viajes, eventos);
        ruta = DatosPrueba.ruta();
        when(sucursales.nombres(any())).thenReturn(NOMBRES);
    }

    private void guardarDevuelveLaMisma() {
        when(repositorio.saveAndFlush(any(RutaEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void sucursalesActivas() {
        when(sucursales.verificarActiva(XELA)).thenReturn(DatosPrueba.vistaSucursal(XELA, "Xela", true));
        when(sucursales.verificarActiva(GUATE)).thenReturn(DatosPrueba.vistaSucursal(GUATE, "Guate", true));
    }

    private static NuevaRutaSolicitud nuevaRuta(UUID origen, UUID destino) {
        return new NuevaRutaSolicitud(origen, destino, new BigDecimal("205.00"), new BigDecimal("75.00"));
    }

    private static ActualizarRutaSolicitud actualizacion(boolean activa, int version) {
        return new ActualizarRutaSolicitud(new BigDecimal("210.50"), new BigDecimal("80.00"), activa, version);
    }

    private static RutaEntidad rutaInactiva() {
        RutaEntidad inactiva = DatosPrueba.ruta();
        inactiva.actualizar(inactiva.getDistanciaKm(), inactiva.getPrecioBoleto(), false);
        return inactiva;
    }


    @Test
    @SuppressWarnings("unchecked")
    void listarOrdenaPorOrigenYDestinoConLosNombresNullAlFinal() {
        RutaEntidad xelaGuate = DatosPrueba.ruta(XELA, GUATE);
        RutaEntidad sinNombreXela = DatosPrueba.ruta(SIN_NOMBRE, XELA);
        RutaEntidad guateXela = DatosPrueba.ruta(GUATE, XELA);
        RutaEntidad xelaAntigua = DatosPrueba.ruta(XELA, ANTIGUA);
        RutaEntidad xelaSinNombre = DatosPrueba.ruta(XELA, SIN_NOMBRE);
        when(repositorio.buscar(XELA, true))
                .thenReturn(List.of(xelaGuate, sinNombreXela, xelaSinNombre, guateXela, xelaAntigua));

        List<RutaRespuesta> rutas = servicio.listar(XELA, true);

        assertThat(rutas).extracting(RutaRespuesta::rutaId).containsExactly(
                guateXela.getId(), xelaAntigua.getId(), xelaGuate.getId(), xelaSinNombre.getId(),
                sinNombreXela.getId());
        assertThat(rutas.get(0).origenNombre()).isEqualTo("Guate");
        assertThat(rutas.get(0).destinoNombre()).isEqualTo("Xela");
        assertThat(rutas.get(4).origenNombre()).isNull();
        ArgumentCaptor<Collection<UUID>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(sucursales).nombres(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(XELA, GUATE, ANTIGUA, SIN_NOMBRE);
    }

    @Test
    void listarSinRutasDevuelveListaVacia() {
        when(repositorio.buscar(null, null)).thenReturn(List.of());

        assertThat(servicio.listar(null, null)).isEmpty();
    }

    @Test
    void obtenerArmaLaRespuestaConLosNombres() {
        when(repositorio.findById(ruta.getId())).thenReturn(Optional.of(ruta));

        RutaRespuesta respuesta = servicio.obtener(ruta.getId());

        assertThat(respuesta.rutaId()).isEqualTo(ruta.getId());
        assertThat(respuesta.sucursalOrigenId()).isEqualTo(XELA);
        assertThat(respuesta.origenNombre()).isEqualTo("Xela");
        assertThat(respuesta.sucursalDestinoId()).isEqualTo(GUATE);
        assertThat(respuesta.destinoNombre()).isEqualTo("Guate");
        assertThat(respuesta.distanciaKm()).isEqualByComparingTo("205.00");
        assertThat(respuesta.precioBoleto()).isEqualByComparingTo("75.00");
        assertThat(respuesta.activa()).isTrue();
        assertThat(respuesta.version()).isZero();
    }

    @Test
    void obtenerPorIdDeUnaRutaQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.obtenerPorId(id), ErrorRuta.RUTA_NO_EXISTE);
    }

    @Test
    void obtenerUnaRutaQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.obtener(id), ErrorRuta.RUTA_NO_EXISTE);
    }

    // ----- T5 -----

    @Test
    void crearDesdeOtraSucursal() {
        UsuarioActual usuario = DatosPrueba.adminSucursal(GUATE);

        VerificarError.lanza(() -> servicio.crear(nuevaRuta(XELA, GUATE), usuario), "SUCURSAL_AJENA");
        verifyNoInteractions(repositorio, eventos);
        verify(sucursales, never()).verificarActiva(any());
    }

    @Test
    void crearConOrigenIgualAlDestino() {
        UsuarioActual usuario = DatosPrueba.adminSucursal(XELA);

        VerificarError.lanza(() -> servicio.crear(nuevaRuta(XELA, XELA), usuario), ErrorRuta.MISMA_SUCURSAL);
        verify(sucursales, never()).verificarActiva(any());
        verifyNoInteractions(repositorio, eventos);
    }

    @Test
    void crearConDestinoInactivo() {
        UsuarioActual usuario = DatosPrueba.adminSucursal(XELA);
        when(sucursales.verificarActiva(XELA)).thenReturn(DatosPrueba.vistaSucursal(XELA, "Xela", true));
        when(sucursales.verificarActiva(GUATE)).thenThrow(ErrorSucursal.SUCURSAL_INACTIVA.excepcion("Guate"));

        VerificarError.lanza(() -> servicio.crear(nuevaRuta(XELA, GUATE), usuario), ErrorSucursal.SUCURSAL_INACTIVA);
        verify(repositorio, never()).saveAndFlush(any(RutaEntidad.class));
        verifyNoInteractions(eventos);
    }

    @Test
    void crearUnaRutaRepetida() {
        UsuarioActual usuario = DatosPrueba.adminSucursal(XELA);
        sucursalesActivas();
        when(repositorio.existsBySucursalOrigenIdAndSucursalDestinoId(XELA, GUATE)).thenReturn(true);

        VerificarError.lanza(() -> servicio.crear(nuevaRuta(XELA, GUATE), usuario), ErrorRuta.RUTA_DUPLICADA);
        verify(repositorio, never()).saveAndFlush(any(RutaEntidad.class));
        verifyNoInteractions(eventos);
    }

    @Test
    void crearGuardaLaRutaYPublicaRutaCreada() {
        UsuarioActual usuario = DatosPrueba.adminSucursal(XELA);
        sucursalesActivas();
        guardarDevuelveLaMisma();

        RutaRespuesta respuesta = servicio.crear(nuevaRuta(XELA, GUATE), usuario);

        ArgumentCaptor<RutaEntidad> guardada = ArgumentCaptor.forClass(RutaEntidad.class);
        verify(repositorio).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getCreadoPor()).isEqualTo(usuario.usuarioId());
        assertThat(guardada.getValue().isActiva()).isTrue();
        assertThat(respuesta.rutaId()).isEqualTo(guardada.getValue().getId());
        assertThat(respuesta.origenNombre()).isEqualTo("Xela");
        assertThat(respuesta.destinoNombre()).isEqualTo("Guate");
        assertThat(respuesta.precioBoleto()).isEqualByComparingTo("75.00");
        verify(eventos).rutaCreada(respuesta);
    }

    @Test
    void elAdminDeSistemaCreaRutasDeCualquierSucursal() {
        sucursalesActivas();
        guardarDevuelveLaMisma();

        RutaRespuesta respuesta = servicio.crear(nuevaRuta(GUATE, XELA), DatosPrueba.adminSistema());

        assertThat(respuesta.sucursalOrigenId()).isEqualTo(GUATE);
        verify(eventos).rutaCreada(respuesta);
    }

    // ----- T6 -----

    @Test
    void actualizarConVersionVieja() {
        when(repositorio.findById(ruta.getId())).thenReturn(Optional.of(ruta));

        VerificarError.lanza(() -> servicio.actualizar(ruta.getId(), actualizacion(true, 3),
                DatosPrueba.adminSucursal(XELA)), ErrorGeneral.MODIFICACION_CONCURRENTE);
        verify(repositorio, never()).saveAndFlush(any(RutaEntidad.class));
        verifyNoInteractions(eventos);
    }

    @Test
    void actualizarUnaRutaDeOtraSucursal() {
        when(repositorio.findById(ruta.getId())).thenReturn(Optional.of(ruta));

        VerificarError.lanza(() -> servicio.actualizar(ruta.getId(), actualizacion(true, 0),
                DatosPrueba.adminSucursal(GUATE)), "SUCURSAL_AJENA");
        verify(repositorio, never()).saveAndFlush(any(RutaEntidad.class));
    }

    @Test
    void actualizarUnaRutaQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.actualizar(id, actualizacion(true, 0), DatosPrueba.adminSistema()),
                ErrorRuta.RUTA_NO_EXISTE);
    }

    @Test
    void reactivarVerificaQueAmbasSucursalesSiganActivas() {
        RutaEntidad inactiva = rutaInactiva();
        when(repositorio.findById(inactiva.getId())).thenReturn(Optional.of(inactiva));
        sucursalesActivas();
        guardarDevuelveLaMisma();

        RutaRespuesta respuesta = servicio.actualizar(inactiva.getId(), actualizacion(true, 0),
                DatosPrueba.adminSucursal(XELA));

        verify(sucursales).verificarActiva(XELA);
        verify(sucursales).verificarActiva(GUATE);
        assertThat(respuesta.activa()).isTrue();
        verify(eventos).rutaActualizada(respuesta);
    }

    @Test
    void reactivarConUnaSucursalInactiva() {
        RutaEntidad inactiva = rutaInactiva();
        when(repositorio.findById(inactiva.getId())).thenReturn(Optional.of(inactiva));
        when(sucursales.verificarActiva(XELA)).thenReturn(DatosPrueba.vistaSucursal(XELA, "Xela", true));
        when(sucursales.verificarActiva(GUATE)).thenThrow(ErrorSucursal.SUCURSAL_INACTIVA.excepcion("Guate"));

        VerificarError.lanza(() -> servicio.actualizar(inactiva.getId(), actualizacion(true, 0),
                DatosPrueba.adminSucursal(XELA)), ErrorSucursal.SUCURSAL_INACTIVA);
        assertThat(inactiva.isActiva()).isFalse();
        verify(repositorio, never()).saveAndFlush(any(RutaEntidad.class));
        verifyNoInteractions(eventos);
    }
}
