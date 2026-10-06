package gt.cunoc.proyecto2.identity.controllers.archivos;


import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Map;


/**
 * Subida de foto de perfil (I10). No cambia el perfil: la URL se guarda con I9.
 */
@RestController
@RequestMapping("/identity/archivos")
public class ArchivosController {

    private final AlmacenFotos almacen;

    public ArchivosController(AlmacenFotos almacen) {
        this.almacen = almacen;
    }

    @PostMapping("/fotos")
    public ResponseEntity<Map<String, String>> subir(@RequestParam("archivo") MultipartFile archivo) {
        String url = almacen.subir("perfiles", archivo);
        return ResponseEntity.created(URI.create(url)).body(Map.of("fotoUrl", url));
    }
}
