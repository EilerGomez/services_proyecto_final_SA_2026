package gt.cunoc.proyecto2.fleet.servicios.fotos;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import gt.cunoc.proyecto2.fleet.errores.fotos.ErrorFoto;
import gt.cunoc.proyecto2.fleet.modelos.fotos.FormatoImagen;
import gt.cunoc.proyecto2.fleet.modelos.fotos.TipoFoto;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementación con Google Cloud Storage.
 * Credenciales por ADC: en la VM, la cuenta de servicio de la VM; en local,
 * "gcloud auth application-default login". Nunca una llave JSON en el repositorio.
 * El cliente de GCS se crea en la primera subida: así el servicio arranca aunque no haya credenciales.
 */
@Service
public class AlmacenFotosServicioImpl implements AlmacenFotosServicio {

    private static final Logger log = LoggerFactory.getLogger(AlmacenFotosServicioImpl.class);
    private static final long MAXIMO_BYTES = 2L * 1024 * 1024;
    private static final String CACHE_UN_ANIO = "public, max-age=31536000";

    private final String bucket;
    private final String prefijo;
    private volatile Storage storage;

    public AlmacenFotosServicioImpl(@Value("${app.fotos.bucket:proyecto-final-sa-fotos}") String bucket,
                                    @Value("${app.fotos.prefijo:}") String prefijo) {
        this.bucket = bucket;
        this.prefijo = prefijo;
    }

    @Override
    public String subir(TipoFoto tipo, MultipartFile archivo) {
        byte[] contenido = leer(archivo);
        FormatoImagen formato = FormatoImagen.detectar(contenido)
                .orElseThrow(() -> ErrorFoto.ARCHIVO_INVALIDO.excepcion());

        // Nombre = UUID: no se puede adivinar ni choca con otro
        String objeto = prefijo + tipo.carpeta() + "/" + UUID.randomUUID() + "." + formato.extension();
        BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucket, objeto))
                .setContentType(formato.tipoMime())
                .setCacheControl(CACHE_UN_ANIO)
                .build();
        try {
            storage().create(info, contenido);
        } catch (RuntimeException e) {          // StorageException, credenciales, red
            log.error("No se pudo subir {} al bucket {}", objeto, bucket, e);
            throw ErrorFoto.ALMACENAMIENTO_NO_DISPONIBLE.excepcion();
        }
        return urlBase() + objeto;
    }

    @Override
    public void verificarUrl(String fotoUrl) {
        if (fotoUrl == null || !fotoUrl.startsWith(urlBase())) {
            throw ErrorFoto.FOTO_INVALIDA.excepcion();
        }
    }

    private static byte[] leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAXIMO_BYTES) {
            throw ErrorFoto.ARCHIVO_INVALIDO.excepcion();
        }
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw ErrorFoto.ARCHIVO_INVALIDO.excepcion();
        }
    }

    private String urlBase() {
        return "https://storage.googleapis.com/" + bucket + "/";
    }

    private Storage storage() {
        Storage actual = storage;
        if (actual == null) {
            synchronized (this) {
                if (storage == null) {
                    storage = StorageOptions.getDefaultInstance().getService();
                }
                actual = storage;
            }
        }
        return actual;
    }
}