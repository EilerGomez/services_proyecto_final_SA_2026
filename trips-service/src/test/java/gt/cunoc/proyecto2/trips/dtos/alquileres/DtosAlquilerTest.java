package gt.cunoc.proyecto2.trips.dtos.alquileres;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ConfiguracionAlquilerEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.RecorridoAlquiler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtosAlquilerTest {

    private static final ZoneOffset GUATEMALA = ZoneOffset.ofHours(-6);
    private static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-05T07:00:00-06:00");
    private static final OffsetDateTime RETORNO = OffsetDateTime.parse("2026-11-05T19:00:00-06:00");

    private static NuevoAlquilerSolicitud solicitud(String nit, String correo, OffsetDateTime retorno) {
        return new NuevoAlquilerSolicitud(DatosPrueba.XELA, nit, "  Ana López ", correo, " Quetzaltenango ",
                " Antigua Guatemala  ", SALIDA, retorno, 30, new BigDecimal("200.00"), DatosPrueba.FECHA,
                null, null, null, null);
    }

    // ----- NuevaConfiguracionAlquilerSolicitud y su respuesta -----

    @Test
    void nuevaConfiguracionCreaLaEntidad() {
        UUID creadoPor = UUID.randomUUID();
        NuevaConfiguracionAlquilerSolicitud solicitud = new NuevaConfiguracionAlquilerSolicitud(
                new BigDecimal("13.75"), DatosPrueba.FECHA);

        ConfiguracionAlquilerEntidad configuracion = solicitud.crearEntidad(creadoPor);

        assertThat(configuracion.getId()).isNull();
        assertThat(configuracion.getPrecioPorKm()).isEqualByComparingTo("13.75");
        assertThat(configuracion.getVigenteDesde()).isEqualTo(DatosPrueba.FECHA);
        assertThat(configuracion.getCreadoPor()).isEqualTo(creadoPor);
    }

    @Test
    void respuestaDeConfiguracion() {
        ConfiguracionAlquilerEntidad configuracion = DatosPrueba.configuracion("13.75", DatosPrueba.FECHA);

        ConfiguracionAlquilerRespuesta respuesta = new ConfiguracionAlquilerRespuesta(configuracion);

        assertThat(respuesta.configuracionId()).isEqualTo(configuracion.getId()).isNotNull();
        assertThat(respuesta.precioPorKm()).isEqualByComparingTo("13.75");
        assertThat(respuesta.vigenteDesde()).isEqualTo(DatosPrueba.FECHA);
        assertThat(respuesta.creadoPor()).isEqualTo(configuracion.getCreadoPor());
    }

    // ----- NuevoAlquilerSolicitud -----

    @Test
    void elConstructorNormalizaLosTextos() {
        NuevoAlquilerSolicitud solicitud = solicitud(" 1234567-k ", "  Ana.Lopez@Correo.COM ", RETORNO);

        assertThat(solicitud.clienteNit()).isEqualTo("1234567-K");
        assertThat(solicitud.clienteNombre()).isEqualTo("Ana López");
        assertThat(solicitud.clienteCorreo()).isEqualTo("ana.lopez@correo.com");
        assertThat(solicitud.origen()).isEqualTo("Quetzaltenango");
        assertThat(solicitud.destino()).isEqualTo("Antigua Guatemala");
    }

    @Test
    void nitYCorreoVaciosQuedanNull() {
        NuevoAlquilerSolicitud solicitud = solicitud("   ", "", RETORNO);

        assertThat(solicitud.clienteNit()).isNull();
        assertThat(solicitud.clienteCorreo()).isNull();
    }

    @Test
    void textosNullSeQuedanNull() {
        NuevoAlquilerSolicitud solicitud = new NuevoAlquilerSolicitud(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);

        assertThat(solicitud.clienteNit()).isNull();
        assertThat(solicitud.clienteNombre()).isNull();
        assertThat(solicitud.clienteCorreo()).isNull();
        assertThat(solicitud.origen()).isNull();
        assertThat(solicitud.destino()).isNull();
    }

    @Test
    void recorridoConLosDatosDeLaSolicitud() {
        RecorridoAlquiler recorrido = solicitud(null, null, RETORNO).recorrido();

        assertThat(recorrido.origen()).isEqualTo("Quetzaltenango");
        assertThat(recorrido.destino()).isEqualTo("Antigua Guatemala");
        assertThat(recorrido.fechaSalida()).isEqualTo(SALIDA);
        assertThat(recorrido.fechaRetorno()).isEqualTo(RETORNO);
        assertThat(recorrido.cantidadPasajeros()).isEqualTo(30);
        assertThat(recorrido.kmEstimados()).isEqualByComparingTo("200.00");
        assertThat(recorrido.origenLatitud()).isNull();
        assertThat(recorrido.destinoLongitud()).isNull();
    }

    @Test
    void recorridoGuardaLasCoordenadasDelMapa() {
        NuevoAlquilerSolicitud solicitud = new NuevoAlquilerSolicitud(DatosPrueba.XELA, null, "Ana López", null,
                "Quetzaltenango", "Antigua Guatemala", SALIDA, RETORNO, 30, new BigDecimal("200.00"),
                DatosPrueba.FECHA, new BigDecimal("14.834700"), new BigDecimal("-91.518400"),
                new BigDecimal("14.558600"), new BigDecimal("-90.729500"));

        RecorridoAlquiler recorrido = solicitud.recorrido();

        assertThat(recorrido.origenLatitud()).isEqualByComparingTo("14.834700");
        assertThat(recorrido.origenLongitud()).isEqualByComparingTo("-91.518400");
        assertThat(recorrido.destinoLatitud()).isEqualByComparingTo("14.558600");
        assertThat(recorrido.destinoLongitud()).isEqualByComparingTo("-90.729500");
    }

    @Test
    void coordenadasIncompletasSonInvalidas() {
        NuevoAlquilerSolicitud sinLongitud = new NuevoAlquilerSolicitud(DatosPrueba.XELA, null, "Ana López", null,
                "Quetzaltenango", "Antigua Guatemala", SALIDA, RETORNO, 30, new BigDecimal("200.00"),
                DatosPrueba.FECHA, new BigDecimal("14.834700"), null, null, null);

        VerificarError.lanza(sinLongitud::recorrido, ErrorAlquiler.COORDENADAS_INCOMPLETAS);
    }

    @Test
    void coordenadasFueraDeRangoSonInvalidas() {
        NuevoAlquilerSolicitud fueraDeRango = new NuevoAlquilerSolicitud(DatosPrueba.XELA, null, "Ana López", null,
                "Quetzaltenango", "Antigua Guatemala", SALIDA, RETORNO, 30, new BigDecimal("200.00"),
                DatosPrueba.FECHA, new BigDecimal("91"), new BigDecimal("-91"), null, null);

        VerificarError.lanza(fueraDeRango::recorrido, ErrorAlquiler.COORDENADAS_FUERA_DE_RANGO);
    }

    @Test
    void recorridoConRetornoAntesDeLaSalida() {
        NuevoAlquilerSolicitud solicitud = solicitud(null, null, SALIDA.minusHours(1));

        VerificarError.lanza(solicitud::recorrido, ErrorAlquiler.RETORNO_ANTES_DE_SALIDA);
    }

    @Test
    void enLineaSinCorreoUsaElDelToken() {
        UUID clienteId = UUID.randomUUID();
        UUID solicitadoPor = UUID.randomUUID();

        AlquilerPrivadoEntidad alquiler = solicitud(null, null, null).crearEntidad(CanalAlquiler.EN_LINEA, clienteId,
                "token@correo.com", new BigDecimal("2750.00"), solicitadoPor);

        assertThat(alquiler.getCanal()).isEqualTo(CanalAlquiler.EN_LINEA);
        assertThat(alquiler.getClienteUsuarioId()).isEqualTo(clienteId);
        assertThat(alquiler.getClienteCorreo()).isEqualTo("token@correo.com");
        assertThat(alquiler.getClienteNit()).isEqualTo("CF");
        assertThat(alquiler.getClienteNombre()).isEqualTo("Ana López");
        assertThat(alquiler.getSucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(alquiler.getOrigen()).isEqualTo("Quetzaltenango");
        assertThat(alquiler.getDestino()).isEqualTo("Antigua Guatemala");
        assertThat(alquiler.getFechaSalida()).isEqualTo(SALIDA);
        assertThat(alquiler.getFechaRetorno()).isNull();
        assertThat(alquiler.getCantidadPasajeros()).isEqualTo(30);
        assertThat(alquiler.getKmEstimados()).isEqualByComparingTo("200.00");
        assertThat(alquiler.getPrecioEstimado()).isEqualByComparingTo("2750.00");
        assertThat(alquiler.getFechaSolicitud()).isEqualTo(DatosPrueba.FECHA);
        assertThat(alquiler.getSolicitadoPor()).isEqualTo(solicitadoPor);
        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.SOLICITADO);
    }

    @Test
    void enLineaConCorreoPropioLoConserva() {
        AlquilerPrivadoEntidad alquiler = solicitud("1234567-8", "otro@correo.com", RETORNO).crearEntidad(
                CanalAlquiler.EN_LINEA, UUID.randomUUID(), "token@correo.com", new BigDecimal("2750.00"),
                UUID.randomUUID());

        assertThat(alquiler.getClienteCorreo()).isEqualTo("otro@correo.com");
        assertThat(alquiler.getClienteNit()).isEqualTo("1234567-8");
    }

    @Test
    void presencialSinCorreoNoUsaElDelToken() {
        AlquilerPrivadoEntidad alquiler = solicitud(null, null, RETORNO).crearEntidad(CanalAlquiler.PRESENCIAL,
                null, "cajero@sucursal.gt", new BigDecimal("2750.00"), UUID.randomUUID());

        assertThat(alquiler.getCanal()).isEqualTo(CanalAlquiler.PRESENCIAL);
        assertThat(alquiler.getClienteUsuarioId()).isNull();
        assertThat(alquiler.getClienteCorreo()).isNull();
    }

    @Test
    void crearEntidadConRetornoAntesDeLaSalidaEsInvalido() {
        NuevoAlquilerSolicitud solicitud = solicitud(null, null, SALIDA.minusMinutes(1));

        VerificarError.lanza(() -> solicitud.crearEntidad(CanalAlquiler.PRESENCIAL, null, null,
                new BigDecimal("2750.00"), UUID.randomUUID()), ErrorAlquiler.RETORNO_ANTES_DE_SALIDA);
    }

    // ----- CancelarAlquilerSolicitud -----

    @Test
    void cancelarNormalizaElMotivo() {
        assertThat(new CancelarAlquilerSolicitud("  Cambio de planes ", null, 0).motivo())
                .isEqualTo("Cambio de planes");
        assertThat(new CancelarAlquilerSolicitud(null, null, 0).motivo()).isNull();
    }

    // ----- AlquilerRespuesta -----

    @Test
    void respuestaDeAlquilerCotizado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        UUID admin = UUID.randomUUID();
        alquiler.cotizar(new BigDecimal("2600.00"), admin);
        UUID viajeId = UUID.randomUUID();

        AlquilerRespuesta respuesta = new AlquilerRespuesta(alquiler, viajeId);

        assertThat(respuesta.alquilerId()).isEqualTo(alquiler.getId());
        assertThat(respuesta.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(respuesta.canal()).isEqualTo(CanalAlquiler.EN_LINEA);
        assertThat(respuesta.clienteUsuarioId()).isEqualTo(alquiler.getClienteUsuarioId());
        assertThat(respuesta.clienteNit()).isEqualTo("1234567-8");
        assertThat(respuesta.clienteNombre()).isEqualTo("Ana López");
        assertThat(respuesta.clienteCorreo()).isEqualTo("cliente@correo.com");
        assertThat(respuesta.origen()).isEqualTo("Quetzaltenango");
        assertThat(respuesta.destino()).isEqualTo("Antigua Guatemala");
        assertThat(respuesta.origenLatitud()).isEqualByComparingTo("14.834700");
        assertThat(respuesta.origenLongitud()).isEqualByComparingTo("-91.518400");
        assertThat(respuesta.destinoLatitud()).isEqualByComparingTo("14.558600");
        assertThat(respuesta.destinoLongitud()).isEqualByComparingTo("-90.729500");
        assertThat(respuesta.fechaSalida()).isEqualTo(SALIDA);
        assertThat(respuesta.fechaRetorno()).isEqualTo(RETORNO);
        assertThat(respuesta.cantidadPasajeros()).isEqualTo(30);
        assertThat(respuesta.kmEstimados()).isEqualByComparingTo("200.00");
        assertThat(respuesta.precioEstimado()).isEqualByComparingTo("2750.00");
        assertThat(respuesta.precioConfirmado()).isEqualByComparingTo("2600.00");
        assertThat(respuesta.confirmadoPor()).isEqualTo(admin);
        assertThat(respuesta.estado()).isEqualTo(EstadoAlquiler.COTIZADO);
        assertThat(respuesta.motivoCancelacion()).isNull();
        assertThat(respuesta.fechaSolicitud()).isEqualTo(DatosPrueba.FECHA);
        assertThat(respuesta.viajeId()).isEqualTo(viajeId);
        assertThat(respuesta.version()).isZero();
    }

    @Test
    void respuestaDeAlquilerSoloIdaEnHoraDeGuatemala() {
        // 13:00 UTC = 07:00 en Guatemala
        NuevoAlquilerSolicitud solicitud = new NuevoAlquilerSolicitud(DatosPrueba.XELA, null, "Juan", null, "Xela",
                "Guate", OffsetDateTime.of(2026, 11, 5, 13, 0, 0, 0, ZoneOffset.UTC), null, 10,
                new BigDecimal("205.00"), DatosPrueba.FECHA, null, null, null, null);
        AlquilerPrivadoEntidad alquiler = solicitud.crearEntidad(CanalAlquiler.PRESENCIAL, null, null,
                new BigDecimal("2818.75"), UUID.randomUUID());
        alquiler.cancelar("Sin presupuesto", OffsetDateTime.parse("2026-11-01T10:00:00-06:00"));

        AlquilerRespuesta respuesta = new AlquilerRespuesta(alquiler, null);

        assertThat(respuesta.fechaSalida()).isEqualTo(OffsetDateTime.of(2026, 11, 5, 7, 0, 0, 0, GUATEMALA));
        assertThat(respuesta.fechaRetorno()).isNull();
        assertThat(respuesta.estado()).isEqualTo(EstadoAlquiler.CANCELADO);
        assertThat(respuesta.motivoCancelacion()).isEqualTo("Sin presupuesto");
        assertThat(respuesta.precioConfirmado()).isNull();
        assertThat(respuesta.viajeId()).isNull();
        assertThat(respuesta.version()).isNull();
    }
}
