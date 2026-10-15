package gt.cunoc.proyecto2.identity.archivos;

import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Subida de fotos de perfil a Azure Blob Storage (contenedor con lectura pública por URL).
 * URL de cada foto: &lt;cuenta-url&gt;/&lt;contenedor&gt;/&lt;prefijo&gt;perfiles/&lt;uuid&gt;.&lt;ext&gt;
 * Credenciales: identidad administrada de la VM (o un SAS solo en local). Ver {@link ClienteBlobAzure}.
 */
@Component
public class AlmacenFotos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenFotos.class);

    private static final long MAXIMO_BYTES = 2L * 1024 * 1024;

    private static final Map<String, String> EXTENSIONES =
            Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final ClienteBlobAzure blob;

    private final String prefijo;

    @Autowired
    public AlmacenFotos(
            @Value("${app.fotos.cuenta-url}") String cuentaUrl,
            @Value("${app.fotos.contenedor:fotos}") String contenedor,
            @Value("${app.fotos.prefijo:}") String prefijo,
            @Value("${app.fotos.sas:}") String sas,
            @Value("${app.fotos.url-identidad:http://169.254.169.254/metadata/identity/oauth2/token}")
            String urlIdentidad) {
        this(new ClienteBlobAzure(cuentaUrl, contenedor, sas, urlIdentidad,
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)   // IMDS y Blob: HTTP/1.1 sin "upgrade"
                        .connectTimeout(Duration.ofSeconds(5))
                        .build(), Clock.systemUTC()), prefijo);
    }

    AlmacenFotos(ClienteBlobAzure blob, String prefijo) {
        this.blob = blob;
        this.prefijo = prefijo == null ? "" : prefijo;
    }

    /** Sube la foto y devuelve su URL pública. carpeta: "perfiles", "buses" o "choferes". */
    public String subir(String carpeta, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAXIMO_BYTES) {
            throw new SolicitudInvalidaException("ARCHIVO_INVALIDO", "La foto es obligatoria y debe pesar máximo 2 MB");
        }
        String tipo = archivo.getContentType();
        String extension = tipo == null ? null : EXTENSIONES.get(tipo);
        if (extension == null) {
            throw new SolicitudInvalidaException("ARCHIVO_INVALIDO", "Solo se permiten fotos JPG, PNG o WEBP");
        }
        String objeto = prefijo + carpeta + "/" + UUID.randomUUID() + "." + extension;
        try {
            blob.subir(objeto, archivo.getBytes(), tipo);
        } catch (IOException e) {
            log.error("No se pudo subir la foto {}: {}", objeto, e.getMessage());
            throw new ServicioNoDisponibleException("ALMACENAMIENTO_NO_DISPONIBLE", "No se pudo guardar la foto, intente de nuevo");
        }
        return blob.urlContenedor() + objeto;
    }

    /** true si la URL es de nuestro contenedor de fotos (para validar lo que manda el frontend). */
    public boolean esDelBucket(String url) {
        return url != null && url.startsWith(blob.urlContenedor());
    }

    /**
     * Borra una foto que ya no se usa (porque se reemplazó). Solo si es de nuestro contenedor: las de Google
     * u otros sitios no se tocan. Dentro de una transacción espera al commit, para no borrar una foto
     * que sigue en la base si la transacción se revierte. Es "mejor esfuerzo": si Azure falla, se registra
     * en el log y no se interrumpe la operación (la foto queda huérfana, sin efecto para el usuario).
     */
    public void eliminarAlConfirmar(String fotoUrl) {
        if (!esDelBucket(fotoUrl)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eliminar(fotoUrl);
                }
            });
        } else {
            eliminar(fotoUrl);
        }
    }

    private void eliminar(String fotoUrl) {
        String objeto = fotoUrl.substring(blob.urlContenedor().length());
        try {
            blob.eliminar(objeto);
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo borrar la foto anterior {}: {}", objeto, e.getMessage());
        }
    }
}
