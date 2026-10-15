package gt.cunoc.proyecto2.identity.archivos;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Ramas de AlmacenFotos: nulo, exceso de tamaño y fallos del almacenamiento. */
@ExtendWith(MockitoExtension.class)
class AlmacenFotosRamasTest {

    @Mock private ClienteBlobAzure blob;

    private AlmacenFotos almacen() {
        return new AlmacenFotos(blob, "dev/");
    }

    @Test
    void subirNuloDa400() {
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException.class,
                () -> almacen().subir("perfiles", null));

        assertEquals("ARCHIVO_INVALIDO", error.getCodigo());
    }

    @Test
    void subirExcedeTamanoDa400() {
        var archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", new byte[(2 * 1024 * 1024) + 1]);

        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException.class,
                () -> almacen().subir("perfiles", archivo));

        assertEquals("ARCHIVO_INVALIDO", error.getCodigo());
    }

    @Test
    void errorAlmacenamientoDa503() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});
        doThrow(new IOException("Azure Blob respondió 403")).when(blob).subir(anyString(), any(), anyString());

        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException.class,
                () -> almacen().subir("perfiles", archivo));

        assertEquals("ALMACENAMIENTO_NO_DISPONIBLE", error.getCodigo());
    }

    @Test
    void errorLecturaDa503() throws Exception {
        MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.isEmpty()).thenReturn(false);
        when(archivo.getSize()).thenReturn(3L);
        when(archivo.getContentType()).thenReturn("image/png");
        when(archivo.getBytes()).thenThrow(new IOException("disco"));

        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException.class,
                () -> almacen().subir("perfiles", archivo));

        assertEquals("ALMACENAMIENTO_NO_DISPONIBLE", error.getCodigo());
    }
}
