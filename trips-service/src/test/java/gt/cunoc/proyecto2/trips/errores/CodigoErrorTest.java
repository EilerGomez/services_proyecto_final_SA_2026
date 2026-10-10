package gt.cunoc.proyecto2.trips.errores;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorAlquiler;
import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorConfiguracionAlquiler;
import gt.cunoc.proyecto2.trips.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.trips.errores.integraciones.ErrorIntegracion;
import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
import gt.cunoc.proyecto2.trips.errores.rutas.ErrorRuta;
import gt.cunoc.proyecto2.trips.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CodigoErrorTest {

    private static final Map<TipoError, Integer> STATUS = Map.of(
            TipoError.SOLICITUD_INVALIDA, 400,
            TipoError.PROHIBIDO, 403,
            TipoError.NO_ENCONTRADO, 404,
            TipoError.CONFLICTO, 409,
            TipoError.REGLA_NEGOCIO, 422,
            TipoError.SERVICIO_NO_DISPONIBLE, 503);

    static Stream<CodigoError> todosLosErrores() {
        List<CodigoError> errores = new ArrayList<>();
        errores.addAll(List.of(ErrorGeneral.values()));
        errores.addAll(List.of(ErrorSucursal.values()));
        errores.addAll(List.of(ErrorRuta.values()));
        errores.addAll(List.of(ErrorViaje.values()));
        errores.addAll(List.of(ErrorOperacionViaje.values()));
        errores.addAll(List.of(ErrorAlquiler.values()));
        errores.addAll(List.of(ErrorConfiguracionAlquiler.values()));
        errores.addAll(List.of(ErrorIntegracion.values()));
        return errores.stream();
    }

    /** Sin datos el mensaje no pasa por String.format: sirve aunque tenga %s o %d. */
    @ParameterizedTest
    @MethodSource("todosLosErrores")
    void cadaErrorCreaSuExcepcionConCodigoStatusYMensaje(CodigoError error) {
        NegocioException excepcion = error.excepcion();

        assertThat(excepcion.getCodigo()).isEqualTo(error.name()).isEqualTo(error.codigo());
        assertThat(excepcion.getStatus().value()).isEqualTo(STATUS.get(error.tipo()));
        assertThat(excepcion.getMessage()).isEqualTo(error.mensaje()).isNotBlank();
    }

    @Test
    void elMensajeSeLlenaConLosDatos() {
        assertThat(ErrorRuta.RUTA_DUPLICADA.excepcion("Xela", "Guate").getMessage())
                .isEqualTo("Ya existe una ruta de Xela a Guate");
        assertThat(ErrorViaje.CAPACIDAD_MENOR.excepcion(30, 45).getMessage())
                .isEqualTo("El bus nuevo tiene 30 asientos y el viaje tiene 45: ya se pudieron vender esos asientos");
        assertThat(ErrorAlquiler.CAPACIDAD_INSUFICIENTE.excepcion(20, 30).getMessage())
                .isEqualTo("El bus tiene 20 asientos y el alquiler es para 30 pasajeros");
        assertThat(ErrorOperacionViaje.KM_INVALIDO.excepcion(new BigDecimal("900.00"), new BigDecimal("1000.00"))
                .getMessage())
                .isEqualTo("El kilometraje final (900.00) es menor que el inicial (1000.00)");
        assertThat(ErrorConfiguracionAlquiler.SIN_PRECIO_ALQUILER.excepcion(LocalDate.of(2026, 10, 20))
                .getMessage())
                .isEqualTo("No hay un precio por km de alquiler vigente el 2026-10-20");
        assertThat(ErrorViaje.RANGO_FECHAS_INVALIDO.excepcion(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 20))
                .getMessage())
                .isEqualTo("La fecha desde (2026-10-21) es posterior a la fecha hasta (2026-10-20)");
    }

    @Test
    void cadaTipoCreaLaExcepcionDeComun() {
        assertThat(TipoError.SOLICITUD_INVALIDA.crear("C", "m")).isInstanceOf(SolicitudInvalidaException.class);
        assertThat(TipoError.PROHIBIDO.crear("C", "m")).isInstanceOf(ProhibidoException.class);
        assertThat(TipoError.NO_ENCONTRADO.crear("C", "m")).isInstanceOf(NoEncontradoException.class);
        assertThat(TipoError.CONFLICTO.crear("C", "m")).isInstanceOf(ConflictoException.class);
        assertThat(TipoError.REGLA_NEGOCIO.crear("C", "m")).isInstanceOf(ReglaNegocioException.class);
        assertThat(TipoError.SERVICIO_NO_DISPONIBLE.crear("C", "m"))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
