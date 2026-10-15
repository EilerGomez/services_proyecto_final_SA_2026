package gt.cunoc.proyecto2.fleet.controladores.fotos;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.fleet.dtos.fotos.FotoSubidaRespuesta;
import gt.cunoc.proyecto2.fleet.modelos.fotos.TipoFoto;
import gt.cunoc.proyecto2.fleet.servicios.fotos.AlmacenFotosServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/fleet/archivos")
@Tag(name = "Fotos", description = "Subida de fotos de buses y choferes al almacenamiento de fotos")
public class FotoControlador {

    private final AlmacenFotosServicio fotos;

    public FotoControlador(AlmacenFotosServicio fotos) {
        this.fotos = fotos;
    }

    @PostMapping(value = "/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "F22 · Sube una foto (JPG, PNG o WEBP, máx. 2 MB) y devuelve su URL")
    public ResponseEntity<FotoSubidaRespuesta> subir(@RequestParam TipoFoto tipo,
                                                     @RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new FotoSubidaRespuesta(fotos.subir(tipo, archivo)));
    }
}