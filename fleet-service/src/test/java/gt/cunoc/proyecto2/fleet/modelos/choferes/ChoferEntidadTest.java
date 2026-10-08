package gt.cunoc.proyecto2.fleet.modelos.choferes;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.errores.choferes.ErrorChofer;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChoferEntidadTest {

    private final ChoferEntidad chofer = DatosPrueba.chofer(UUID.randomUUID());

    @Test
    void unChoferNuevoQuedaActivo() {
        assertThat(chofer.getEstado()).isEqualTo(EstadoChofer.ACTIVO);
        assertThat(chofer.esActivo()).isTrue();
        assertThat(chofer.getNumeroLicencia()).isEqualTo("LIC-1234");
    }

    @Test
    void actualizaSalarioYLicencia() {
        chofer.actualizarSalario(new BigDecimal("400.00"));
        chofer.actualizarLicencia("LIC-9999", TipoLicencia.B, FECHA.plusYears(5));

        assertThat(chofer.getSalarioBaseViaje()).isEqualByComparingTo("400.00");
        assertThat(chofer.getNumeroLicencia()).isEqualTo("LIC-9999");
        assertThat(chofer.getTipoLicencia()).isEqualTo(TipoLicencia.B);
        assertThat(chofer.getFechaVencimientoLicencia()).isEqualTo(FECHA.plusYears(5));
    }

    @Test
    void unChoferInactivoNoSeModifica() {
        chofer.desactivar();

        assertThat(chofer.getEstado()).isEqualTo(EstadoChofer.INACTIVO);
        assertThat(chofer.esActivo()).isFalse();
        VerificarError.lanza(() -> chofer.actualizarSalario(BigDecimal.TEN), ErrorChofer.CHOFER_INACTIVO);
        VerificarError.lanza(() -> chofer.actualizarLicencia("X-1234", TipoLicencia.A, FECHA),
                ErrorChofer.CHOFER_INACTIVO);
        VerificarError.lanza(chofer::desactivar, ErrorChofer.CHOFER_INACTIVO);
    }

    @Test
    void elNombreSoloCambiaSiEsDistintoAunqueEsteInactivo() {
        assertThat(chofer.actualizarNombre("Juan Perez")).isFalse();

        chofer.desactivar();

        assertThat(chofer.actualizarNombre("Juan Carlos Perez")).isTrue();
        assertThat(chofer.getNombreCompleto()).isEqualTo("Juan Carlos Perez");
    }

    @Test
    void laLicenciaVenceAlFinalDelDia() {
        var vencimiento = chofer.getFechaVencimientoLicencia();

        assertThat(chofer.licenciaVigenteEn(vencimiento.minusDays(1))).isTrue();
        assertThat(chofer.licenciaVigenteEn(vencimiento)).isTrue();
        assertThat(chofer.licenciaVigenteEn(vencimiento.plusDays(1))).isFalse();
    }
}
