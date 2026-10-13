package gt.cunoc.proyecto2.maintenance.dtos.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Reglas de @Valid del cuerpo de M5 (las mismas que aplica Spring antes del controlador → 400 VALIDACION). */
class NuevoServicioSolicitudTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabrica.close();
    }

    private static LineaServicioSolicitud linea(String monto) {
        return new LineaServicioSolicitud(UUID.randomUUID(), "4 llantas", "REPUESTO", new BigDecimal(monto));
    }

    private static NuevoServicioSolicitud solicitud(List<LineaServicioSolicitud> lineas) {
        return new NuevoServicioSolicitud(UUID.randomUUID(), LocalDate.of(2026, 10, 22), "Taller", null, lineas);
    }

    private static Set<ConstraintViolation<NuevoServicioSolicitud>> errores(NuevoServicioSolicitud solicitud) {
        return validador.validate(solicitud);
    }

    @Test
    void solicitudCorrectaNoTieneErrores() {
        assertThat(errores(solicitud(List.of(linea("6800.00"), linea("0.01"))))).isEmpty();
    }

    @Test
    void lineaNulaEsInvalida() {
        // Arrays.asList admite null (List.of no): es lo que arma Jackson con "lineas": [null]
        var violaciones = errores(solicitud(Arrays.asList((LineaServicioSolicitud) null)));

        assertThat(violaciones).hasSize(1);
        assertThat(violaciones.iterator().next().getPropertyPath().toString()).startsWith("lineas[0]");
    }

    @Test
    void lineaNulaEntreLineasValidasTambienEsInvalida() {
        assertThat(errores(solicitud(Arrays.asList(linea("10.00"), null)))).hasSize(1);
    }

    @Test
    void montoConTresDecimalesEsInvalido() {
        var violaciones = errores(solicitud(List.of(linea("10.123"))));

        assertThat(violaciones).hasSize(1);
        assertThat(violaciones.iterator().next().getPropertyPath().toString()).isEqualTo("lineas[0].monto");
    }

    @Test
    void montoConMasDeDiezEnterosEsInvalido() {
        assertThat(errores(solicitud(List.of(linea("12345678901.00"))))).hasSize(1);
    }

    @Test
    void montoCeroYSinLineasSiguenSiendoInvalidos() {
        assertThat(errores(solicitud(List.of(linea("0.00"))))).hasSize(1);
        assertThat(errores(solicitud(List.of()))).hasSize(1);
        assertThat(errores(solicitud(null))).hasSize(1);
    }
}
