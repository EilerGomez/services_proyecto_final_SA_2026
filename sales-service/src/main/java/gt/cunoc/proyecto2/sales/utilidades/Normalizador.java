package gt.cunoc.proyecto2.sales.utilidades;

import java.util.Locale;

public final class Normalizador {

    private Normalizador() {
    }

    public static String texto(String valor) {
        return valor == null ? null : valor.strip();
    }

    public static String textoOpcional(String valor) {
        String limpio = texto(valor);
        return limpio == null || limpio.isEmpty() ? null : limpio;
    }

    public static String codigo(String valor) {
        String limpio = texto(valor);
        return limpio == null ? null : limpio.toUpperCase(Locale.ROOT);
    }

    public static String correo(String valor) {
        String limpio = texto(valor);
        return limpio == null ? null : limpio.toLowerCase(Locale.ROOT);
    }
}