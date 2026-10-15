package gt.cunoc.proyecto2.identity.controllers.archivos;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.archivos.AlmacenFotos;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchivosController.class)
class ArchivosControllerTest extends BaseControladorTest {

    @MockitoBean
    private AlmacenFotos almacen;

    @Test
    void subirFotoDevuelveUrl() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.CLIENTE), null));
        when(almacen.subir(eq("perfiles"), any())).thenReturn("https://stproyecto2fotos.blob.core.windows.net/fotos/perfiles/foto.jpg");
        var archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});

        // Act + Assert
        mvc.perform(multipart("/identity/archivos/fotos").file(archivo)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fotoUrl").value("https://stproyecto2fotos.blob.core.windows.net/fotos/perfiles/foto.jpg"));
    }
}
