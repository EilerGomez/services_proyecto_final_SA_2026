package gt.cunoc.proyecto2.wallet.models;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de prueba sin setters: las entidades se crean con sus constructores y fábricas, el saldo se carga
 * con {@code acreditar} y el id (que en la aplicación pone la base de datos) se asigna por reflexión.
 */
public final class DatosPrueba {

    private DatosPrueba() {
    }

    /** Cartera de cliente ya guardada, con el saldo indicado. */
    public static Cartera cartera(UUID usuarioId, String saldo) {
        Cartera cartera = conId(new Cartera(usuarioId));
        cartera.acreditar(new BigDecimal(saldo));
        return cartera;
    }

    public static Cartera cartera(String saldo) {
        return cartera(UUID.randomUUID(), saldo);
    }

    /** Billetera del sistema ya guardada, con el saldo indicado. */
    public static Cartera sistema(String saldo) {
        Cartera sistema = conId(Cartera.delSistema());
        sistema.acreditar(new BigDecimal(saldo));
        return sistema;
    }

    /** Le asigna un id, como si la entidad ya estuviera guardada. */
    public static <T> T conId(T entidad) {
        ReflectionTestUtils.setField(entidad, "id", UUID.randomUUID());
        return entidad;
    }
}
