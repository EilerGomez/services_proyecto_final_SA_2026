package gt.cunoc.proyecto2.fleet.modelos.fotos;

import java.util.Optional;

/**
 * Formatos de foto permitidos. Se detectan por los primeros bytes del archivo ("firma"),
 * no por el Content-Type que manda el navegador, que se puede falsificar.
 */
public enum FormatoImagen {

    JPG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String tipoMime;
    private final String extension;

    FormatoImagen(String tipoMime, String extension) {
        this.tipoMime = tipoMime;
        this.extension = extension;
    }

    public String tipoMime() {
        return tipoMime;
    }

    public String extension() {
        return extension;
    }

    public static Optional<FormatoImagen> detectar(byte[] contenido) {
        if (empiezaCon(contenido, 0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPG);
        }
        if (empiezaCon(contenido, 0, 0x89, 'P', 'N', 'G')) {
            return Optional.of(PNG);
        }
        if (empiezaCon(contenido, 0, 'R', 'I', 'F', 'F') && empiezaCon(contenido, 8, 'W', 'E', 'B', 'P')) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean empiezaCon(byte[] contenido, int desde, int... firma) {
        if (contenido == null || contenido.length < desde + firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((contenido[desde + i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}