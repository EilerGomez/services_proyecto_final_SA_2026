package gt.cunoc.proyecto2.wallet.pagos;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProveedorSimuladoTest {

    private final ProveedorSimulado proveedor = new ProveedorSimulado();

    @Test
    void apruebaConReferenciaDerivadaDelIntento() {
        UUID intento = UUID.randomUUID();

        ResultadoCobro resultado = proveedor.cobrar(
                new SolicitudCobro(intento, UUID.randomUUID(), new BigDecimal("100.00"), SolicitudCobro.QUETZALES));

        assertThat(proveedor.codigo()).isEqualTo(ProveedorSimulado.CODIGO);
        assertThat(resultado.aprobado()).isTrue();
        assertThat(resultado.referenciaExterna()).isEqualTo("SIM-" + intento);
        assertThat(resultado.motivo()).isNull();
    }

    @Test
    void resultadoRechazadoNoTraeReferencia() {
        ResultadoCobro rechazo = ResultadoCobro.rechazado("FONDOS_INSUFICIENTES");

        assertThat(rechazo.aprobado()).isFalse();
        assertThat(rechazo.referenciaExterna()).isNull();
        assertThat(rechazo.motivo()).isEqualTo("FONDOS_INSUFICIENTES");
    }
}
