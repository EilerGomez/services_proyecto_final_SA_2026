package gt.cunoc.proyecto2.fleet.servicios.fotos;

import gt.cunoc.proyecto2.fleet.clientes.almacenamiento.ClienteBlobAzure;
import gt.cunoc.proyecto2.fleet.errores.fotos.ErrorFoto;
import gt.cunoc.proyecto2.fleet.modelos.fotos.FormatoImagen;
import gt.cunoc.proyecto2.fleet.modelos.fotos.TipoFoto;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementación con Azure Blob Storage (contenedor con lectura pública por URL).
 * Credenciales: en la VM, la identidad administrada de la VM; en local, un SAS del contenedor
 * (FOTOS_SAS, solo en el .env local). Nunca una clave de la cuenta en el repositorio.
 * No se conecta a Azure al arrancar: solo en cada subida.
 */
@Service
public class AlmacenFotosServicioImpl implements AlmacenFotosServicio {

    private static final Logger log = LoggerFactory.getLogger(AlmacenFotosServicioImpl.class);
    private static final long MAXIMO_BYTES = 2L * 1024 * 1024;

    private final ClienteBlobAzure blob;
    private final String prefijo;

    @Autowired
    public AlmacenFotosServicioImpl(
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

    AlmacenFotosServicioImpl(ClienteBlobAzure blob, String prefijo) {
        this.blob = blob;
        this.prefijo = prefijo == null ? "" : prefijo;
    }

    @Override
    public String subir(TipoFoto tipo, MultipartFile archivo) {
        byte[] contenido = leer(archivo);
        FormatoImagen formato = FormatoImagen.detectar(contenido)
                .orElseThrow(() -> ErrorFoto.ARCHIVO_INVALIDO.excepcion());

        // Nombre = UUID: no se puede adivinar ni choca con otro
        String objeto = prefijo + tipo.carpeta() + "/" + UUID.randomUUID() + "." + formato.extension();
        try {
            blob.subir(objeto, contenido, formato.tipoMime());
        } catch (IOException | RuntimeException e) {     // Azure rechazó, sin identidad, sin red
            log.error("No se pudo subir {} a {}: {}", objeto, blob.urlContenedor(), e.getMessage());
            throw ErrorFoto.ALMACENAMIENTO_NO_DISPONIBLE.excepcion();
        }
        return blob.urlContenedor() + objeto;
    }

    @Override
    public void verificarUrl(String fotoUrl) {
        if (fotoUrl == null || fotoUrl.isBlank()) {
            throw ErrorFoto.FOTO_OBLIGATORIA.excepcion();
        }
        if (!fotoUrl.startsWith(blob.urlContenedor())) {
            throw ErrorFoto.FOTO_INVALIDA.excepcion();
        }
    }

    @Override
    public void eliminarAlConfirmar(String fotoUrl) {
        if (fotoUrl == null || !fotoUrl.startsWith(blob.urlContenedor())) {
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
}
