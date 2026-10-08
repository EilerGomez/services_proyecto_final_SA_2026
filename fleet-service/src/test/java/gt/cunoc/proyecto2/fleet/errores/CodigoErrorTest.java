package gt.cunoc.proyecto2.fleet.errores;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import gt.cunoc.proyecto2.fleet.errores.bitacora.ErrorBitacora;
import gt.cunoc.proyecto2.fleet.errores.buses.ErrorBus;
import gt.cunoc.proyecto2.fleet.errores.choferes.ErrorChofer;
import gt.cunoc.proyecto2.fleet.errores.fotos.ErrorFoto;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.fleet.errores.integraciones.ErrorIntegracion;
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
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
        errores.addAll(List.of(ErrorPersonal.values()));
        errores.addAll(List.of(ErrorBus.values()));
        errores.addAll(List.of(ErrorChofer.values()));
        errores.addAll(List.of(ErrorFoto.values()));
        errores.addAll(List.of(ErrorBitacora.values()));
        errores.addAll(List.of(ErrorIntegracion.values()));
        return errores.stream();
    }

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
        assertThat(ErrorSucursal.SUCURSAL_DUPLICADA.excepcion("código", "XELA").getMessage())
                .isEqualTo("Ya existe una sucursal con el código XELA");
        assertThat(ErrorBus.BUS_CON_VIAJES_ACTIVOS.excepcion(2).getMessage())
                .isEqualTo("No se puede desactivar: el bus tiene 2 viaje(s) programado(s) o en ruta");
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
