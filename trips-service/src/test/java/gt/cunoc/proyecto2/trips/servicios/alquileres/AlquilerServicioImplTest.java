package gt.cunoc.proyecto2.trips.servicios.alquileres;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AlquilerRespuesta;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AsignarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CancelarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CotizarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.FiltroAlquileres;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevoAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.NombresViaje;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorAlquiler;
import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorConfiguracionAlquiler;
import gt.cunoc.proyecto2.trips.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.trips.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.trips.eventos.PublicadorEventosTrips;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ClienteAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.AlquilerPrivadoRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.servicios.sucursales.VistaSucursalServicio;
import gt.cunoc.proyecto2.trips.servicios.viajes.AsignacionViajeServicio;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class AlquilerServicioImplTest {

    /** Fechas relativas al día real de Guatemala: el servicio compara con Fechas.hoy(). */
    private static final LocalDate MANANA = LocalDate.now(ZoneId.of("America/Guatemala")).plusDays(1);
    private static final LocalDate AYER = LocalDate.now(ZoneId.of("America/Guatemala")).minusDays(1);

    private static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-05T07:00:00-06:00");
    private static final OffsetDateTime RETORNO = OffsetDateTime.parse("2026-11-05T19:00:00-06:00");
    private static final OffsetDateTime LLEGADA_ESTIMADA = OffsetDateTime.parse("2026-11-05T11:00:00-06:00");

    private final AlquilerPrivadoRepositorio repositorio = mock(AlquilerPrivadoRepositorio.class);
    private final ConfiguracionAlquilerServicio precios = mock(ConfiguracionAlquilerServicio.class);
    private final VistaSucursalServicio sucursales = mock(VistaSucursalServicio.class);
    private final AsignacionViajeServicio asignaciones = mock(AsignacionViajeServicio.class);
    private final ViajeServicio viajes = mock(ViajeServicio.class);
    private final ViajeRepositorio viajeRepositorio = mock(ViajeRepositorio.class);
    private final PublicadorEventosTrips eventos = mock(PublicadorEventosTrips.class);

    private AlquilerServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new AlquilerServicioImpl(repositorio, precios, sucursales, asignaciones, viajes,
                viajeRepositorio, eventos);
    }

    private void guardarDevuelveElMismo() {
        when(repositorio.saveAndFlush(any(AlquilerPrivadoEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static NuevoAlquilerSolicitud nuevoAlquiler(UUID sucursalId, String correo, OffsetDateTime retorno,
                                                        LocalDate fechaSolicitud) {
        return new NuevoAlquilerSolicitud(sucursalId, null, "Ana López", correo, "Quetzaltenango",
                "Antigua Guatemala", SALIDA, retorno, 30, new BigDecimal("123.45"), fechaSolicitud);
    }

    /*Alquiler COTIZADO (aun sin pagar) */
    private static AlquilerPrivadoEntidad alquilerCotizado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        alquiler.cotizar(new BigDecimal("2600.00"), UUID.randomUUID());
        return alquiler;
    }

    private void existe(AlquilerPrivadoEntidad alquiler) {
        when(repositorio.findById(alquiler.getId())).thenReturn(Optional.of(alquiler));
    }

    private static ViajeDetalleRespuesta detalleDe(ViajeEntidad viaje) {
        NombresViaje nombres = new NombresViaje(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        return new ViajeDetalleRespuesta(new ViajeRespuesta(viaje, nombres), null, null);
    }

    // solictar un nuevo alquiler

    @Test
    void elCajeroSolicitaEnMostradorEnSuSucursal() {
        UsuarioActual cajero = DatosPrueba.cajero(XELA);
        when(precios.vigenteEn(MANANA)).thenReturn(DatosPrueba.configuracion("10.00", AYER));
        guardarDevuelveElMismo();

        AlquilerRespuesta respuesta = servicio.solicitar(nuevoAlquiler(XELA, null, RETORNO, MANANA), cajero);

        verify(sucursales).verificarActiva(XELA);
        assertThat(respuesta.canal()).isEqualTo(CanalAlquiler.PRESENCIAL);
        assertThat(respuesta.clienteUsuarioId()).isNull();
        assertThat(respuesta.clienteCorreo()).isNull();
        assertThat(respuesta.clienteNit()).isEqualTo(ClienteAlquiler.CONSUMIDOR_FINAL);
        assertThat(respuesta.estado()).isEqualTo(EstadoAlquiler.SOLICITADO);
        assertThat(respuesta.sucursalId()).isEqualTo(XELA);
        assertThat(respuesta.fechaSolicitud()).isEqualTo(MANANA);
        assertThat(respuesta.viajeId()).isNull();
        assertThat(respuesta.precioEstimado()).isEqualByComparingTo("1234.50");
        verifyNoInteractions(viajeRepositorio, eventos);
    }

    @Test
    void elClienteSolicitaEnLineaConElCorreoDelToken() {
        UsuarioActual cliente = DatosPrueba.cliente();
        when(precios.vigenteEn(MANANA)).thenReturn(DatosPrueba.configuracion("12.00", AYER));
        guardarDevuelveElMismo();

        AlquilerRespuesta respuesta = servicio.solicitar(nuevoAlquiler(GUATE, null, null, MANANA), cliente);

        verify(sucursales).verificarActiva(GUATE);
        assertThat(respuesta.canal()).isEqualTo(CanalAlquiler.EN_LINEA);
        assertThat(respuesta.clienteUsuarioId()).isEqualTo(cliente.usuarioId());
        assertThat(respuesta.clienteCorreo()).isEqualTo("cliente@correo.com");
        assertThat(respuesta.fechaRetorno()).isNull();

        assertThat(respuesta.precioEstimado()).isEqualByComparingTo("1481.40");
        ArgumentCaptor<AlquilerPrivadoEntidad> guardado = ArgumentCaptor.forClass(AlquilerPrivadoEntidad.class);
        verify(repositorio).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getSolicitadoPor()).isEqualTo(cliente.usuarioId());
    }

    @Test
    void elClienteEnLineaConservaElCorreoQueEscribio() {
        when(precios.vigenteEn(MANANA)).thenReturn(DatosPrueba.configuracion("10.00", AYER));
        guardarDevuelveElMismo();

        AlquilerRespuesta respuesta = servicio.solicitar(
                nuevoAlquiler(XELA, "  Otro@Correo.com ", RETORNO, MANANA), DatosPrueba.cliente());

        assertThat(respuesta.clienteCorreo()).isEqualTo("otro@correo.com");
    }

    @Test
    void solicitarEnUnaSucursalInactiva() {
        when(sucursales.verificarActiva(XELA)).thenThrow(ErrorSucursal.SUCURSAL_INACTIVA.excepcion("Xela"));

        VerificarError.lanza(() -> servicio.solicitar(nuevoAlquiler(XELA, null, RETORNO, MANANA),
                DatosPrueba.cliente()), ErrorSucursal.SUCURSAL_INACTIVA);
        verifyNoInteractions(precios, repositorio);
    }

    @Test
    void solicitarConRetornoAntesDeLaSalida() {
        VerificarError.lanza(() -> servicio.solicitar(nuevoAlquiler(XELA, null, SALIDA.minusHours(1), MANANA),
                DatosPrueba.cliente()), ErrorAlquiler.RETORNO_ANTES_DE_SALIDA);
        verifyNoInteractions(precios, repositorio);
    }

    @Test
    void solicitarConFechaDeSolicitudPasada() {
        VerificarError.lanza(() -> servicio.solicitar(nuevoAlquiler(XELA, null, RETORNO, AYER),
                DatosPrueba.cliente()), ErrorAlquiler.FECHA_SOLICITUD_PASADA);
        verifyNoInteractions(precios, repositorio);
    }

    @Test
    void solicitarSinPrecioVigente() {
        when(precios.vigenteEn(MANANA)).thenThrow(ErrorConfiguracionAlquiler.SIN_PRECIO_ALQUILER.excepcion(MANANA));

        VerificarError.lanza(() -> servicio.solicitar(nuevoAlquiler(XELA, null, RETORNO, MANANA),
                DatosPrueba.cliente()), ErrorConfiguracionAlquiler.SIN_PRECIO_ALQUILER);
        verify(repositorio, never()).saveAndFlush(any(AlquilerPrivadoEntidad.class));
    }


    @Test
    void listarSinAlquileresNoBuscaViajes() {
        when(repositorio.findAll(ArgumentMatchers.<Specification<AlquilerPrivadoEntidad>>any(),
                eq(Sort.by("fechaSalida")))).thenReturn(List.of());

        assertThat(servicio.listar(new FiltroAlquileres(null, EstadoAlquiler.PAGADO, null, null))).isEmpty();
        verifyNoInteractions(viajeRepositorio);
    }

    @Test
    void miosSinAlquileres() {
        UsuarioActual cliente = DatosPrueba.cliente();
        when(repositorio.findByClienteUsuarioIdOrderByFechaSalidaDesc(cliente.usuarioId())).thenReturn(List.of());

        assertThat(servicio.mios(cliente)).isEmpty();
        verifyNoInteractions(viajeRepositorio);
    }

    @Test
    void listarIncluyeElViajeAsignadoConBusYChofer() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();
        ViajeEntidad viaje = DatosPrueba.viajePrivado(alquiler, DatosPrueba.asignacion());
        when(repositorio.findAll(ArgumentMatchers.<Specification<AlquilerPrivadoEntidad>>any(),
                eq(Sort.by("fechaSalida")))).thenReturn(List.of(alquiler));
        when(viajeRepositorio.findByAlquilerIdIn(List.of(alquiler.getId()))).thenReturn(List.of(viaje));
        when(viajes.resumen(viaje)).thenReturn(detalleDe(viaje).viaje());

        List<AlquilerRespuesta> alquileres = servicio.listar(new FiltroAlquileres(XELA, null, null, null));

        assertThat(alquileres).hasSize(1);
        AlquilerRespuesta respuesta = alquileres.get(0);
        assertThat(respuesta.estado()).isEqualTo(EstadoAlquiler.PAGADO);
        assertThat(respuesta.viajeId()).isEqualTo(viaje.getId());
        assertThat(respuesta.viaje()).isNotNull();
        assertThat(respuesta.viaje().viajeId()).isEqualTo(viaje.getId());
    }

    @Test
    void listarSinViajeAsignadoDejaElViajeEnNulo() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        when(repositorio.findAll(ArgumentMatchers.<Specification<AlquilerPrivadoEntidad>>any(),
                eq(Sort.by("fechaSalida")))).thenReturn(List.of(alquiler));
        when(viajeRepositorio.findByAlquilerIdIn(List.of(alquiler.getId()))).thenReturn(List.of());

        List<AlquilerRespuesta> alquileres = servicio.listar(new FiltroAlquileres(XELA, null, null, null));

        assertThat(alquileres).hasSize(1);
        assertThat(alquileres.get(0).viajeId()).isNull();
        assertThat(alquileres.get(0).viaje()).isNull();
        verifyNoInteractions(viajes);
    }

    // ----- T19 -----

    @Test
    void elDuenoVeSuAlquiler() {
        UsuarioActual cliente = DatosPrueba.cliente();
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler(cliente.usuarioId());
        existe(alquiler);

        AlquilerRespuesta respuesta = servicio.obtener(alquiler.getId(), cliente);

        assertThat(respuesta.alquilerId()).isEqualTo(alquiler.getId());
        assertThat(respuesta.fechaSalida()).isEqualTo(alquiler.getFechaSalida());
        assertThat(respuesta.precioEstimado()).isEqualByComparingTo("2750.00");
        assertThat(respuesta.version()).isZero();
    }

    @Test
    void elPersonalDeLaSucursalVeElAlquiler() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        existe(alquiler);

        assertThat(servicio.obtener(alquiler.getId(), DatosPrueba.cajero(XELA)).alquilerId())
                .isEqualTo(alquiler.getId());
        assertThat(servicio.obtener(alquiler.getId(), DatosPrueba.adminSucursal(XELA)).alquilerId())
                .isEqualTo(alquiler.getId());
    }

    @Test
    void elAdminDeSistemaVeCualquierAlquiler() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        existe(alquiler);

        assertThat(servicio.obtener(alquiler.getId(), DatosPrueba.adminSistema()).alquilerId())
                .isEqualTo(alquiler.getId());
    }

    @Test
    void otroClienteNoVeElAlquiler() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        existe(alquiler);

        VerificarError.lanza(() -> servicio.obtener(alquiler.getId(), DatosPrueba.cliente()),
                ErrorAlquiler.ALQUILER_AJENO);
    }

    @Test
    void elPersonalDeOtraSucursalNoVeElAlquiler() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        existe(alquiler);

        VerificarError.lanza(() -> servicio.obtener(alquiler.getId(), DatosPrueba.cajero(GUATE)),
                ErrorAlquiler.ALQUILER_AJENO);
    }

    @Test
    void obtenerUnAlquilerQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.obtener(id, DatosPrueba.adminSistema()), ErrorAlquiler.ALQUILER_NO_EXISTE);
    }

    // citizar alquiler

    @Test
    void cotizarGuardaYPublicaLaCotizacion() {
        UsuarioActual admin = DatosPrueba.adminSucursal(XELA);
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        existe(alquiler);
        guardarDevuelveElMismo();

        AlquilerRespuesta respuesta = servicio.cotizar(alquiler.getId(),
                new CotizarAlquilerSolicitud(new BigDecimal("2600.00"), 0), admin);

        assertThat(respuesta.estado()).isEqualTo(EstadoAlquiler.COTIZADO);
        assertThat(respuesta.precioConfirmado()).isEqualByComparingTo("2600.00");
        assertThat(respuesta.confirmadoPor()).isEqualTo(admin.usuarioId());
        verify(repositorio).saveAndFlush(alquiler);
        verify(eventos).cotizacionAlquilerConfirmada(alquiler);
    }


    // el duenio del alquiler cancela su viaje

    @Test
    void elDuenoCancelaSuAlquiler() {
        UsuarioActual cliente = DatosPrueba.cliente();
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler(cliente.usuarioId());
        existe(alquiler);
        guardarDevuelveElMismo();

        AlquilerRespuesta respuesta = servicio.cancelar(alquiler.getId(),
                new CancelarAlquilerSolicitud("  Cambio de planes ", 0), cliente);

        assertThat(respuesta.estado()).isEqualTo(EstadoAlquiler.CANCELADO);
        assertThat(respuesta.motivoCancelacion()).isEqualTo("Cambio de planes");
        verify(repositorio).saveAndFlush(alquiler);
        verifyNoInteractions(eventos);
    }

    @Test
    void elAdminDeLaSucursalCancelaElAlquiler() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        existe(alquiler);
        guardarDevuelveElMismo();

        AlquilerRespuesta respuesta = servicio.cancelar(alquiler.getId(),
                new CancelarAlquilerSolicitud("Sin buses", 0), DatosPrueba.adminSucursal(XELA));

        assertThat(respuesta.estado()).isEqualTo(EstadoAlquiler.CANCELADO);
    }

    @Test
    void elAdminDeSistemaCancelaElAlquiler() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        existe(alquiler);
        guardarDevuelveElMismo();

        AlquilerRespuesta respuesta = servicio.cancelar(alquiler.getId(),
                new CancelarAlquilerSolicitud("Sin buses", 0), DatosPrueba.adminSistema());

        assertThat(respuesta.estado()).isEqualTo(EstadoAlquiler.CANCELADO);
    }

}
