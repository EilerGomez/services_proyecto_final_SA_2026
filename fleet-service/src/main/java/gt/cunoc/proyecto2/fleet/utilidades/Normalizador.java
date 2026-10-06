/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.utilidades;

/**
 *
 * @author eiler
 */
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Normalizador {

    private static final int DECIMALES_COORDENADA = 6;

    private Normalizador() {
    }

    /** Quita los espacios del inicio y del final. Null se queda null. */
    public static String texto(String valor) {
        return valor == null ? null : valor.strip();
    }

    /** Igual que {@link #texto}, pero un texto vacío se convierte en null (campos opcionales). */
    public static String textoOpcional(String valor) {
        String limpio = texto(valor);
        return limpio == null || limpio.isEmpty() ? null : limpio;
    }

    /** Texto sin espacios y en mayúsculas (códigos, placas, licencias). */
    public static String codigo(String valor) {
        String limpio = texto(valor);
        return limpio == null ? null : limpio.toUpperCase(Locale.ROOT);
    }

    /** Redondea latitud o longitud a 6 decimales, lo que admite la columna NUMERIC(9,6). */
    public static BigDecimal coordenada(BigDecimal valor) {
        return valor == null ? null : valor.setScale(DECIMALES_COORDENADA, RoundingMode.HALF_UP);
    }
}
