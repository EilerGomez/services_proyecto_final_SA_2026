package gt.cunoc.proyecto2.wallet.models;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Callbacks JPA en el mismo paquete (son package-private).
class CarteraTest {

    @Test
    void alCrearFijaFechaYAlActualizarLaRefresca() throws Exception {
        // Arrange
        Cartera cartera = new Cartera(UUID.randomUUID());
        Thread.sleep(5);

        // Act
        cartera.alCrear();
        var primera = cartera.getFechaActualizacion();
        Thread.sleep(5);
        cartera.alActualizar();

        // Assert
        assertAll(
                () -> assertNotNull(primera),
                () -> assertTrue(cartera.getFechaActualizacion().isAfter(primera)));
    }

    @Test
    void alCrearRespetaFechaExistente() {
        // Arrange
        Cartera cartera = new Cartera(UUID.randomUUID());
        var fija = Instant.parse("2026-01-01T00:00:00Z");
        ReflectionTestUtils.setField(cartera, "fechaActualizacion", fija);

        // Act
        cartera.alCrear();

        // Assert
        assertEquals(fija, cartera.getFechaActualizacion());
    }

    // ----- Saldo -----

    private static Cartera conSaldo(String saldo, String tipo) {
        Cartera cartera = Cartera.SISTEMA.equals(tipo) ? Cartera.delSistema() : new Cartera(UUID.randomUUID());
        cartera.acreditar(new BigDecimal(saldo));
        return cartera;
    }

    @Test
    void seCreaConSaldoCeroYConSuTipo() {
        UUID usuarioId = UUID.randomUUID();
        Cartera cliente = new Cartera(usuarioId);
        Cartera sistema = Cartera.delSistema();

        assertAll(
                () -> assertEquals(usuarioId, cliente.getUsuarioId()),
                () -> assertEquals(Cartera.USUARIO, cliente.getTipo()),
                () -> assertEquals(0, BigDecimal.ZERO.compareTo(cliente.getSaldo())),
                () -> assertTrue(sistema.esSistema()),
                () -> assertEquals(null, sistema.getUsuarioId()));
    }

    @Test
    void acreditarYDebitarCambianElSaldo() {
        Cartera cartera = conSaldo("100.00", Cartera.USUARIO);

        cartera.acreditar(new BigDecimal("50.00"));
        cartera.debitar(new BigDecimal("150.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(cartera.getSaldo()));
    }

    @Test
    void debitarMasDelSaldoNoLoDejaNegativo() {
        Cartera cartera = conSaldo("10.00", Cartera.USUARIO);

        NegocioException error = assertThrows(NegocioException.class, () -> cartera.debitar(new BigDecimal("10.01")));

        assertEquals("SALDO_NEGATIVO", error.getCodigo());
        assertEquals(0, new BigDecimal("10.00").compareTo(cartera.getSaldo()));
    }

    @Test
    void enLaBilleteraDelSistemaElSaldoInsuficienteTieneSuPropioCodigo() {
        Cartera sistema = conSaldo("10.00", Cartera.SISTEMA);

        NegocioException error = assertThrows(NegocioException.class, () -> sistema.debitar(new BigDecimal("20.00")));

        assertEquals("SALDO_SISTEMA_INSUFICIENTE", error.getCodigo());
    }

    @Test
    void noSePuedePasarDelSaldoMaximo() {
        Cartera cartera = conSaldo("9999999999.00", Cartera.USUARIO);

        NegocioException error = assertThrows(NegocioException.class,
                () -> cartera.acreditar(new BigDecimal("1.00")));

        assertEquals("SALDO_EXCEDE_LIMITE", error.getCodigo());
        assertEquals(0, new BigDecimal("9999999999.00").compareTo(cartera.getSaldo()));
    }

    @Test
    void validarAcreditacionNoCambiaElSaldo() {
        Cartera cartera = conSaldo("100.00", Cartera.USUARIO);

        cartera.validarAcreditacion(new BigDecimal("50.00"));

        assertEquals(0, new BigDecimal("100.00").compareTo(cartera.getSaldo()));
        assertThrows(NegocioException.class, () -> cartera.validarAcreditacion(Cartera.SALDO_MAXIMO));
    }

    @Test
    void alcanzaParaComparaContraElSaldo() {
        Cartera cartera = conSaldo("100.00", Cartera.USUARIO);

        assertTrue(cartera.alcanzaPara(new BigDecimal("100.00")));
        assertFalse(cartera.alcanzaPara(new BigDecimal("100.01")));
    }
}
