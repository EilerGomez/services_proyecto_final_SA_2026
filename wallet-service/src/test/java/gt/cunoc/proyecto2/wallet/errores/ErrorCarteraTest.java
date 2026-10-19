package gt.cunoc.proyecto2.wallet.errores;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCarteraTest {

    @ParameterizedTest
    @EnumSource(ErrorCartera.class)
    void elCodigoEsElNombreDeLaConstante(ErrorCartera error) {
        assertThat(error.codigo()).isEqualTo(error.name());
        assertThat(error.excepcion("x").getCodigo()).isEqualTo(error.name());
    }

    @Test
    void cadaTipoLanzaSuExcepcion() {
        assertThat(ErrorCartera.CARTERA_NO_EXISTE.excepcion()).isInstanceOf(NoEncontradoException.class);
        assertThat(ErrorCartera.MONTO_RECARGA_INVALIDO.excepcion()).isInstanceOf(SolicitudInvalidaException.class);
        assertThat(ErrorCartera.SALDO_NEGATIVO.excepcion()).isInstanceOf(ReglaNegocioException.class);
        assertThat(ErrorCartera.IDENTITY_NO_DISPONIBLE.excepcion()).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void elMensajeSeRellenaConLosDatos() {
        assertThat(ErrorCartera.PAGO_RECHAZADO.excepcion("Tarjeta rechazada").getMessage())
                .isEqualTo("Tarjeta rechazada");
        assertThat(ErrorCartera.SALDO_EXCEDE_LIMITE.excepcion("100.00").getMessage())
                .isEqualTo("La cartera no puede superar un saldo de Q100.00");
    }

    @Test
    void losMotivosDeRechazoConservanSusCodigos() {
        assertThat(MotivoRechazoDebito.values()).extracting(Enum::name)
                .containsExactly("SALDO_INSUFICIENTE", "USUARIO_INACTIVO", "CARTERA_INEXISTENTE");
    }
}
