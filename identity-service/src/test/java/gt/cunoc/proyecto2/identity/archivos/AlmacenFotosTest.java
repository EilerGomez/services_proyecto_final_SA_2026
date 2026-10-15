package gt.cunoc.proyecto2.identity.archivos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AlmacenFotosTest {

    private static final String CONTENEDOR = "https://cuenta.blob.core.windows.net/fotos/";

    @Mock private ClienteBlobAzure blob;

    private AlmacenFotos almacen() {
        lenient().when(blob.urlContenedor()).thenReturn(CONTENEDOR);
        return new AlmacenFotos(blob, "dev/");
    }

    @Test
    void springCreaElBeanYSubeAAzureConLaIdentidadAdministrada() {
        try (AzureFalso azure = AzureFalso.iniciar()) {
            new ApplicationContextRunner()
                    .withUserConfiguration(AlmacenFotos.class)
                    .withPropertyValues(
                            "app.fotos.cuenta-url=" + azure.url(),
                            "app.fotos.contenedor=fotos",
                            "app.fotos.prefijo=dev/",
                            "app.fotos.url-identidad=" + azure.url() + "/token")
                    .run(contexto -> {
                        assertThat(contexto).hasNotFailed().hasSingleBean(AlmacenFotos.class);
                        var archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});

                        String url = contexto.getBean(AlmacenFotos.class).subir("perfiles", archivo);

                        assertThat(url).startsWith(azure.url() + "/fotos/dev/perfiles/").endsWith(".jpg");
                        assertThat(azure.pedidosDeToken()).hasSize(1);
                        assertThat(azure.subidas().get(0).cabecera("Authorization")).isEqualTo("Bearer tok-1");
                        assertThat(azure.subidas().get(0).cuerpo()).containsExactly(1, 2, 3);
                    });
        }
    }

    @Test
    void subirDevuelveUrlPublica() throws Exception {
        // Arrange
        var archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});

        // Act
        String url = almacen().subir("perfiles", archivo);

        // Assert
        assertAll(
                () -> assertTrue(url.startsWith(CONTENEDOR + "dev/perfiles/")),
                () -> assertTrue(url.endsWith(".jpg")),
                () -> verify(blob).subir(startsWith("dev/perfiles/"), eq(new byte[]{1, 2, 3}), eq("image/jpeg")));
    }

    @Test
    void sinPrefijoLaFotoQuedaEnLaCarpeta() throws Exception {
        lenient().when(blob.urlContenedor()).thenReturn(CONTENEDOR);
        var archivo = new MockMultipartFile("archivo", "foto.webp", "image/webp", new byte[]{1});

        String url = new AlmacenFotos(blob, null).subir("perfiles", archivo);

        assertTrue(url.startsWith(CONTENEDOR + "perfiles/") && url.endsWith(".webp"));
    }

    @Test
    void archivoVacioDa400() {
        // Arrange
        var archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[0]);

        // Act
        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException.class,
                () -> almacen().subir("perfiles", archivo));

        // Assert
        assertEquals("ARCHIVO_INVALIDO", error.getCodigo());
        verifyNoInteractions(blob);
    }

    @Test
    void tipoNoPermitidoOSinTipoDa400() {
        var gif = new MockMultipartFile("archivo", "foto.gif", "image/gif", new byte[]{1});
        var sinTipo = new MockMultipartFile("archivo", "foto", null, new byte[]{1});

        var error = assertThrows(gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException.class,
                () -> almacen().subir("perfiles", gif));
        var error2 = assertThrows(gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException.class,
                () -> almacen().subir("perfiles", sinTipo));

        assertEquals("ARCHIVO_INVALIDO", error.getCodigo());
        assertEquals("ARCHIVO_INVALIDO", error2.getCodigo());
    }

    // ----- Borrado de la foto reemplazada -----

    @Test
    void sinTransaccionBorraLaFotoEnseguida() throws Exception {
        almacen().eliminarAlConfirmar(CONTENEDOR + "dev/perfiles/vieja.jpg");

        verify(blob).eliminar("dev/perfiles/vieja.jpg");
    }

    @Test
    void dentroDeUnaTransaccionEsperaAlCommit() throws Exception {
        AlmacenFotos almacen = almacen();
        TransactionSynchronizationManager.initSynchronization();
        try {
            almacen.eliminarAlConfirmar(CONTENEDOR + "dev/perfiles/vieja.jpg");
            verify(blob, never()).eliminar(anyString());

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            verify(blob).eliminar("dev/perfiles/vieja.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void noBorraFotosAjenasNiNulas() throws Exception {
        AlmacenFotos almacen = almacen();

        almacen.eliminarAlConfirmar("https://lh3.googleusercontent.com/a/foto");
        almacen.eliminarAlConfirmar(null);

        verify(blob, never()).eliminar(anyString());
    }

    @Test
    void siAzureNoPuedeBorrarNoSePropagaElError() throws Exception {
        AlmacenFotos almacen = almacen();
        doThrow(new IOException("Azure Blob respondió 403 al borrar")).when(blob).eliminar(anyString());

        almacen.eliminarAlConfirmar(CONTENEDOR + "dev/perfiles/vieja.jpg");

        verify(blob).eliminar("dev/perfiles/vieja.jpg");
    }

    @Test
    void esDelBucketValidaElContenedor() {
        // Arrange
        AlmacenFotos almacen = almacen();

        // Act + Assert
        assertAll(
                () -> assertTrue(almacen.esDelBucket(CONTENEDOR + "dev/perfiles/a.jpg")),
                () -> assertFalse(almacen.esDelBucket("https://otra.blob.core.windows.net/fotos/a.jpg")),
                () -> assertFalse(almacen.esDelBucket("https://storage.googleapis.com/b/a.jpg")),
                () -> assertFalse(almacen.esDelBucket(null)));
    }
}
