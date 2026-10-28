package gt.cunoc.proyecto2.trips.modelos.alquileres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorAlquiler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlquilerPrivadoEntidadTest {

    /** Momento de la cancelación que indicaría quien cancela (lo manda el frontend). */
    private static final OffsetDateTime CANCELACION = OffsetDateTime.parse("2026-11-01T10:00:00-06:00");

    @Test
    void solicitarCopiaClienteYRecorridoYQuedaSolicitado() {
        UUID usuarioId = UUID.randomUUID();
        UUID solicitadoPor = UUID.randomUUID();
        ClienteAlquiler cliente = new ClienteAlquiler(usuarioId, "1234567-8", "Ana López", "ana@correo.com");
        RecorridoAlquiler recorrido = DatosPrueba.recorrido();

        AlquilerPrivadoEntidad alquiler = AlquilerPrivadoEntidad.solicitar(DatosPrueba.XELA, CanalAlquiler.EN_LINEA,
                cliente, recorrido, new BigDecimal("2750.00"), DatosPrueba.FECHA, solicitadoPor);

        assertThat(alquiler.getId()).isNotNull();
        assertThat(alquiler.getSucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(alquiler.getCanal()).isEqualTo(CanalAlquiler.EN_LINEA);
        assertThat(alquiler.getClienteUsuarioId()).isEqualTo(usuarioId);
        assertThat(alquiler.getClienteNit()).isEqualTo("1234567-8");
        assertThat(alquiler.getClienteNombre()).isEqualTo("Ana López");
        assertThat(alquiler.getClienteCorreo()).isEqualTo("ana@correo.com");
        assertThat(alquiler.getOrigen()).isEqualTo("Quetzaltenango");
        assertThat(alquiler.getDestino()).isEqualTo("Antigua Guatemala");
        assertThat(alquiler.getFechaSalida()).isEqualTo(recorrido.fechaSalida());
        assertThat(alquiler.getFechaRetorno()).isEqualTo(recorrido.fechaRetorno());
        assertThat(alquiler.getCantidadPasajeros()).isEqualTo(30);
        assertThat(alquiler.getKmEstimados()).isEqualByComparingTo("200.00");
        assertThat(alquiler.getPrecioEstimado()).isEqualByComparingTo("2750.00");
        assertThat(alquiler.getPrecioConfirmado()).isNull();
        assertThat(alquiler.getConfirmadoPor()).isNull();
        assertThat(alquiler.getMotivoCancelacion()).isNull();
        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.SOLICITADO);
        assertThat(alquiler.getFechaSolicitud()).isEqualTo(DatosPrueba.FECHA);
        assertThat(alquiler.getSolicitadoPor()).isEqualTo(solicitadoPor);
        assertThat(alquiler.getVersion()).isNull();
    }

    @Test
    void cotizarGuardaElPrecioConfirmado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        UUID admin = UUID.randomUUID();

        alquiler.cotizar(new BigDecimal("2600.00"), admin);

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.COTIZADO);
        assertThat(alquiler.getPrecioConfirmado()).isEqualByComparingTo("2600.00");
        assertThat(alquiler.getConfirmadoPor()).isEqualTo(admin);
    }

    @Test
    void cotizarDosVecesEsEstadoInvalido() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        alquiler.cotizar(new BigDecimal("2600.00"), UUID.randomUUID());

        assertThatThrownBy(() -> alquiler.cotizar(new BigDecimal("2500.00"), UUID.randomUUID()))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorAlquiler.ESTADO_INVALIDO.codigo());
                    assertThat(e.getMessage())
                            .isEqualTo("El alquiler está COTIZADO; esta acción solo aplica a alquileres SOLICITADO");
                });
        assertThat(alquiler.getPrecioConfirmado()).isEqualByComparingTo("2600.00");
    }

    @Test
    void cancelarUnoSolicitado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();

        alquiler.cancelar("Cambio de planes", CANCELACION);

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.CANCELADO);
        assertThat(alquiler.getMotivoCancelacion()).isEqualTo("Cambio de planes");
        assertThat(alquiler.getFechaHoraCancelacion()).isEqualTo(CANCELACION);
    }

    /** Ya no se cancela solo antes de cotizar: también cotizado, pagado y asignado (con reembolso). */
    @Test
    void cancelarDesdeCualquierEstadoAnteriorACompletado() {
        for (EstadoAlquiler estado : EstadoAlquiler.CANCELABLES) {
            AlquilerPrivadoEntidad alquiler = enEstado(estado);

            alquiler.cancelar("Cambio de planes", CANCELACION);

            assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.CANCELADO);
        }
    }

    @Test
    void cancelarUnoCompletadoNoSePuede() {
        AlquilerPrivadoEntidad alquiler = enEstado(EstadoAlquiler.ASIGNADO);
        alquiler.completar();

        VerificarError.lanza(() -> alquiler.cancelar("tarde", CANCELACION), ErrorAlquiler.ESTADO_NO_CANCELABLE);
        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.COMPLETADO);
        assertThat(alquiler.getMotivoCancelacion()).isNull();
    }

    @Test
    void cancelarDosVecesNoSePuede() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        alquiler.cancelar("Cambio de planes", CANCELACION);

        VerificarError.lanza(() -> alquiler.cancelar("otra vez", CANCELACION),
                ErrorAlquiler.ESTADO_NO_CANCELABLE);
    }

    /** Lleva el alquiler hasta el estado indicado por el camino normal. */
    private static AlquilerPrivadoEntidad enEstado(EstadoAlquiler estado) {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        if (estado == EstadoAlquiler.SOLICITADO) {
            return alquiler;
        }
        alquiler.cotizar(new BigDecimal("2600.00"), UUID.randomUUID());
        if (estado == EstadoAlquiler.COTIZADO) {
            return alquiler;
        }
        alquiler.marcarPagado();
        if (estado == EstadoAlquiler.PAGADO) {
            return alquiler;
        }
        alquiler.asignar();
        return alquiler;
    }

    @Test
    void marcarPagadoSoloDesdeCotizado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();

        assertThat(alquiler.marcarPagado()).isFalse();
        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.SOLICITADO);

        alquiler.cotizar(new BigDecimal("2600.00"), UUID.randomUUID());
        assertThat(alquiler.marcarPagado()).isTrue();
        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.PAGADO);

        // evento repetido
        assertThat(alquiler.marcarPagado()).isFalse();
        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.PAGADO);
    }

    @Test
    void verificarPagado() {
        AlquilerPrivadoEntidad pagado = DatosPrueba.alquilerPagado();
        assertThatCode(pagado::verificarPagado).doesNotThrowAnyException();

        AlquilerPrivadoEntidad solicitado = DatosPrueba.alquiler();
        assertThatThrownBy(solicitado::verificarPagado)
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorAlquiler.ALQUILER_NO_PAGADO.codigo());
                    assertThat(e.getMessage()).startsWith("El alquiler está SOLICITADO");
                });
    }

    @Test
    void asignarPasaDePagadoAAsignado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();

        alquiler.asignar();

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.ASIGNADO);
    }

    @Test
    void asignarSinPagarEsAlquilerNoPagado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler();
        alquiler.cotizar(new BigDecimal("2600.00"), UUID.randomUUID());

        VerificarError.lanza(alquiler::asignar, ErrorAlquiler.ALQUILER_NO_PAGADO);
        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.COTIZADO);
    }

    @Test
    void liberarRegresaDeAsignadoAPagado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();
        alquiler.asignar();

        alquiler.liberar();

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.PAGADO);
    }

    @Test
    void liberarSinAsignarNoCambiaNada() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();

        alquiler.liberar();

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.PAGADO);
    }

    @Test
    void completarPasaDeAsignadoACompletado() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();
        alquiler.asignar();

        alquiler.completar();

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.COMPLETADO);
    }

    @Test
    void completarSinAsignarNoCambiaNada() {
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquilerPagado();

        alquiler.completar();

        assertThat(alquiler.getEstado()).isEqualTo(EstadoAlquiler.PAGADO);
    }

    @Test
    void esDelCliente() {
        UUID usuarioId = UUID.randomUUID();
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler(usuarioId);

        assertThat(alquiler.esDelCliente(usuarioId)).isTrue();
        assertThat(alquiler.esDelCliente(UUID.randomUUID())).isFalse();
        assertThat(alquiler.esDelCliente(null)).isFalse();
    }

    @Test
    void unAlquilerPresencialSinCuentaNoEsDeNingunCliente() {
        ClienteAlquiler cliente = new ClienteAlquiler(null, null, "Juan Pérez", null);
        AlquilerPrivadoEntidad alquiler = AlquilerPrivadoEntidad.solicitar(DatosPrueba.XELA, CanalAlquiler.PRESENCIAL,
                cliente, DatosPrueba.recorrido(), new BigDecimal("2750.00"), DatosPrueba.FECHA, UUID.randomUUID());

        assertThat(alquiler.getClienteUsuarioId()).isNull();
        assertThat(alquiler.getClienteNit()).isEqualTo(ClienteAlquiler.CONSUMIDOR_FINAL);
        assertThat(alquiler.esDelCliente(UUID.randomUUID())).isFalse();
    }
}
