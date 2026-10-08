package gt.cunoc.proyecto2.identity.archivos;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AlmacenFotosTest {

    @Mock private Storage storage;

    private AlmacenFotos almacen() {
        return new AlmacenFotos(storage, "mi-bucket", "dev/");
    }

    @Test
    void springCreaElBeanConElConstructorDeConfiguracion() {
        StorageOptions opciones = mock(StorageOptions.class);
        when(opciones.getService()).thenReturn(storage);

        try (var fabrica = mockStatic(StorageOptions.class)) {
            fabrica.when(StorageOptions::getDefaultInstance).thenReturn(opciones);

            new ApplicationContextRunner()
                    .withUserConfiguration(AlmacenFotos.class)
                    .withPropertyValues("app.fotos.bucket=mi-bucket", "app.fotos.prefijo=dev/")
                    .run(contexto -> {
                        assertThat(contexto).hasNotFailed().hasSingleBean(AlmacenFotos.class);
                        var archivo = new MockMultipartFile(
                                "archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});
                        String url = contexto.getBean(AlmacenFotos.class).subir("perfiles", archivo);
                        assertThat(url).startsWith("https://storage.googleapis.com/mi-bucket/dev/perfiles/");
                        verify(storage).create(any(BlobInfo.class), any(byte[].class));
                    });
        }
    }

    @Test
    void subirDevuelveUrlPublica() throws Exception {
        // Arrange
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);
        var archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});

        // Act
        String url = almacen().subir("perfiles", archivo);

        // Assert
        assertAll(
                () -> assertTrue(url.startsWith("https://storage.googleapis.com/mi-bucket/dev/perfiles/")),
                () -> assertTrue(url.endsWith(".jpg")),
                () -> verify(storage).create(any(BlobInfo.class), any(byte[].class)));
    }

    @Test
    void archivoVacioDa400() throws Exception {
        // Arrange
        var archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[0]);

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException.class,
                () -> almacen().subir("perfiles", archivo));

        // Assert
        assertEquals("ARCHIVO_INVALIDO", error.getCodigo());
    }

    @Test
    void tipoNoPermitidoDa400() throws Exception {
        // Arrange
        var archivo = new MockMultipartFile("archivo", "foto.gif", "image/gif", new byte[]{1});

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException.class,
                () -> almacen().subir("perfiles", archivo));

        // Assert
        assertEquals("ARCHIVO_INVALIDO", error.getCodigo());
    }

    @Test
    void esDelBucketValidaPrefijo() throws Exception {
        // Arrange
        AlmacenFotos almacen = almacen();

        // Act + Assert
        assertAll(
                () -> assertTrue(almacen.esDelBucket("https://storage.googleapis.com/mi-bucket/dev/perfiles/a.jpg")),
                () -> assertFalse(almacen.esDelBucket("https://otro.com/foto.jpg")),
                () -> assertFalse(almacen.esDelBucket(null)));
    }
}
