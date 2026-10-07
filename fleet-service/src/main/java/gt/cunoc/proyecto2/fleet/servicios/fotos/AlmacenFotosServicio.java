package gt.cunoc.proyecto2.fleet.servicios.fotos;

import gt.cunoc.proyecto2.fleet.modelos.fotos.TipoFoto;
import org.springframework.web.multipart.MultipartFile;

/** Fotos de buses y choferes en el bucket de Google Cloud Storage. */
public interface AlmacenFotosServicio {

    String subir(TipoFoto tipo, MultipartFile archivo);

    void verificarUrl(String fotoUrl);
}