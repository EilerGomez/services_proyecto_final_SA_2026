package gt.cunoc.proyecto2.fleet.servicios.fotos;

import gt.cunoc.proyecto2.fleet.modelos.fotos.TipoFoto;
import org.springframework.web.multipart.MultipartFile;

/** Fotos de buses y choferes en Azure Blob Storage (contenedor de fotos). */
public interface AlmacenFotosServicio {

    String subir(TipoFoto tipo, MultipartFile archivo);

    void verificarUrl(String fotoUrl);

    /**
     * Borra una foto reemplazada, después del commit. Solo si es de nuestro contenedor; si Azure falla
     * se registra y no interrumpe la operación.
     */
    void eliminarAlConfirmar(String fotoUrl);
}