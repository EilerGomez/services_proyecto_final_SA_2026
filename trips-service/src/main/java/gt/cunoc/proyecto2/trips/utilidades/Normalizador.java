package gt.cunoc.proyecto2.trips.utilidades;

import java.util.Locale;

/**
 * Limpia los datos que llegan en las solicitudes antes de validarlos y guardarlos.
 * Se usa en los constructores compactos de los records de solicitud.
 */
public final class Normalizador {

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

    /** Correo sin espacios y en minúsculas. */
    public static String correo(String valor) {
        String limpio = texto(valor);
        return limpio == null ? null : limpio.toLowerCase(Locale.ROOT);
    }
}