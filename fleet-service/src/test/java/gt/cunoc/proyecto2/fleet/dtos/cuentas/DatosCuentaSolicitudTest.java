package gt.cunoc.proyecto2.fleet.dtos.cuentas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatosCuentaSolicitudTest {

    @Test
    void normalizaLosDatosDeLaCuenta() {
        DatosCuentaSolicitud cuenta = new DatosCuentaSolicitud(null, " Ana@Fleet.GT ", " Ana ", " Lopez ",
                " 1234567-k ", " 1234567890101 ", " 5555 ", " Zona 1 ", "   ");

        assertThat(cuenta.correo()).isEqualTo("ana@fleet.gt");
        assertThat(cuenta.nombres()).isEqualTo("Ana");
        assertThat(cuenta.apellidos()).isEqualTo("Lopez");
        assertThat(cuenta.nit()).isEqualTo("1234567-K");
        assertThat(cuenta.dpi()).isEqualTo("1234567890101");
        assertThat(cuenta.telefono()).isEqualTo("5555");
        assertThat(cuenta.direccion()).isEqualTo("Zona 1");
        assertThat(cuenta.fotoUrl()).isNull();
    }
}
