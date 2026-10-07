package gt.cunoc.proyecto2.fleet.servicios.fotos;

import gt.cunoc.proyecto2.fleet.modelos.fotos.TipoFoto;
import org.springframework.web.multipart.MultipartFile;

/** Fotos de buses y choferes en el bucket de Google Cloud Storage. */
public interface AlmacenFotosServicio {

    /**
     * F22. Sube la foto y devuelve su URL pública.
     * 400 ARCHIVO_INVALIDO (vacía, más de 2 MB o no es JPG/PNG/WEBP), 503 ALMACENAMIENTO_NO_DISPONIBLE.
     */
    String subir(TipoFoto tipo, MultipartFile archivo);

    /** 400 FOTO_INVALIDA si la URL no es de nuestro bucket (F12, F13, F18). */
    void verificarUrl(String fotoUrl);
}