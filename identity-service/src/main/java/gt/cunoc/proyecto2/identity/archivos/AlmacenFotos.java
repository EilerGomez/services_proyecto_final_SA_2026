package gt.cunoc.proyecto2.identity.archivos;

import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Subida de fotos al bucket de Google Cloud Storage (ver 00_convenciones.md §8).
 */
@Component
public class AlmacenFotos {

    private static final long MAXIMO_BYTES = 2L * 1024 * 1024;

    private static final Map<String, String> EXTENSIONES =
            Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final Storage storage;

    private final String bucket;

    private final String prefijo;

    public AlmacenFotos(
            @Value("${app.fotos.bucket}") String bucket,
            @Value("${app.fotos.prefijo:}") String prefijo) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucket = bucket;
        this.prefijo = prefijo;
    }

    /** Sube la foto y devuelve su URL pública. carpeta: "perfiles", "buses" o "choferes". */
    public String subir(String carpeta, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAXIMO_BYTES) {
            throw new SolicitudInvalidaException("ARCHIVO_INVALIDO", "La foto es obligatoria y debe pesar máximo 2 MB");
        }
        String extension = EXTENSIONES.get(archivo.getContentType());
        if (extension == null) {
            throw new SolicitudInvalidaException("ARCHIVO_INVALIDO", "Solo se permiten fotos JPG, PNG o WEBP");
        }
        String objeto = prefijo + carpeta + "/" + UUID.randomUUID() + "." + extension;
        BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucket, objeto))
                .setContentType(archivo.getContentType())
                .setCacheControl("public, max-age=31536000")
                .build();
        try {
            storage.create(info, archivo.getBytes());
        } catch (IOException | StorageException e) {
            throw new ServicioNoDisponibleException("ALMACENAMIENTO_NO_DISPONIBLE", "No se pudo guardar la foto, intente de nuevo");
        }
        return urlBase() + objeto;
    }

    /** true si la URL es de nuestro bucket (para validar lo que manda el frontend). */
    public boolean esDelBucket(String url) {
        return url != null && url.startsWith(urlBase());
    }

    private String urlBase() {
        return "https://storage.googleapis.com/" + bucket + "/";
    }
}
