package gt.cunoc.proyecto2.fleet.servicios.fotos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.clientes.almacenamiento.AzureFalso;
import gt.cunoc.proyecto2.fleet.clientes.almacenamiento.ClienteBlobAzure;
import gt.cunoc.proyecto2.fleet.errores.fotos.ErrorFoto;
import gt.cunoc.proyecto2.fleet.modelos.fotos.TipoFoto;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

class AlmacenFotosServicioImplTest {

    private static final String CONTENEDOR = "https://cuenta.blob.core.windows.net/fotos/";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    private final ClienteBlobAzure blob = mock(ClienteBlobAzure.class);
    private AlmacenFotosServicioImpl servicio;

    @BeforeEach
    void preparar() {
        // Cliente de Azure simulado: así la prueba no necesita credenciales ni red
        when(blob.urlContenedor()).thenReturn(CONTENEDOR);
        servicio = new AlmacenFotosServicioImpl(blob, "dev/");
    }

    private static MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("archivo", "foto.png", "image/png", contenido);
    }

    // ----- F22 -----

    @Test
    void subeLaFotoConNombreUnicoYDevuelveLaUrlPublica() throws IOException {
        String url = servicio.subir(TipoFoto.BUS, archivo(PNG));

        ArgumentCaptor<String> objeto = ArgumentCaptor.forClass(String.class);
        verify(blob).subir(objeto.capture(), eq(PNG), eq("image/png"));
        assertThat(objeto.getValue()).startsWith("dev/buses/").endsWith(".png");
        assertThat(url).isEqualTo(CONTENEDOR + objeto.getValue());
    }

    @Test
    void sinPrefijoLaFotoQuedaEnLaCarpeta() throws IOException {
        String url = new AlmacenFotosServicioImpl(blob, null).subir(TipoFoto.CHOFER, archivo(PNG));

        assertThat(url).startsWith(CONTENEDOR + "choferes/");
    }

    @Test
    void conElConstructorDeConfiguracionSubeConLaIdentidadAdministrada() {
        try (AzureFalso azure = AzureFalso.iniciar()) {
            AlmacenFotosServicioImpl real = new AlmacenFotosServicioImpl(
                    azure.url(), "fotos", "dev/", "", azure.url() + "/token");

            String url = real.subir(TipoFoto.BUS, archivo(PNG));

            assertThat(url).startsWith(azure.url() + "/fotos/dev/buses/").endsWith(".png");
            assertThat(azure.subidas().get(0).cabecera("Authorization")).isEqualTo("Bearer tok-1");
            assertThat(azure.subidas().get(0).cabecera("x-ms-blob-content-type")).isEqualTo("image/png");
        }
    }

    @Test
    void rechazaArchivosVaciosGrandesONulos() {
        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, null), ErrorFoto.ARCHIVO_INVALIDO);
        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, archivo(new byte[0])), ErrorFoto.ARCHIVO_INVALIDO);
        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, archivo(new byte[2 * 1024 * 1024 + 1])),
                ErrorFoto.ARCHIVO_INVALIDO);
        verifyNoMoreInteractions(blob);
    }

    @Test
    void rechazaLoQueNoEsImagenAunqueDigaSerPng() {
        VerificarError.lanza(() -> servicio.subir(TipoFoto.CHOFER, archivo("no soy una imagen".getBytes())),
                ErrorFoto.ARCHIVO_INVALIDO);
        verifyNoMoreInteractions(blob);
    }

    @Test
    void unErrorAlLeerElArchivo() throws IOException {
        MultipartFile roto = mock(MultipartFile.class);
        when(roto.isEmpty()).thenReturn(false);
        when(roto.getSize()).thenReturn(10L);
        when(roto.getBytes()).thenThrow(new IOException("disco"));

        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, roto), ErrorFoto.ARCHIVO_INVALIDO);
    }

    @Test
    void siAzureFallaRespondeServicioNoDisponible() throws IOException {
        doThrow(new IOException("Azure Blob respondió 403")).when(blob).subir(anyString(), any(), anyString());

        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, archivo(PNG)), ErrorFoto.ALMACENAMIENTO_NO_DISPONIBLE);
    }

    @Test
    void unErrorInesperadoTambienEsServicioNoDisponible() throws IOException {
        doThrow(new IllegalStateException("sin red")).when(blob).subir(anyString(), any(), anyString());

        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, archivo(PNG)), ErrorFoto.ALMACENAMIENTO_NO_DISPONIBLE);
    }

    // ----- Borrado de la foto reemplazada -----

    @Test
    void sinTransaccionBorraLaFotoEnseguida() throws IOException {
        servicio.eliminarAlConfirmar(CONTENEDOR + "dev/buses/vieja.png");

        verify(blob).eliminar("dev/buses/vieja.png");
    }

    @Test
    void dentroDeUnaTransaccionEsperaAlCommit() throws IOException {
        TransactionSynchronizationManager.initSynchronization();
        try {
            servicio.eliminarAlConfirmar(CONTENEDOR + "dev/buses/vieja.png");
            verify(blob, never()).eliminar(anyString());

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            verify(blob).eliminar("dev/buses/vieja.png");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void noBorraFotosAjenasNiNulas() throws IOException {
        servicio.eliminarAlConfirmar("https://otro.sitio.com/foto.png");
        servicio.eliminarAlConfirmar(null);

        verify(blob, never()).eliminar(anyString());
    }

    @Test
    void siAzureNoPuedeBorrarNoSePropagaElError() throws IOException {
        doThrow(new IOException("Azure Blob respondió 403 al borrar")).when(blob).eliminar(anyString());

        assertThatCode(() -> servicio.eliminarAlConfirmar(CONTENEDOR + "dev/buses/vieja.png"))
                .doesNotThrowAnyException();
    }

    @Test
    void laUrlDebeSerDeNuestroContenedor() {
        VerificarError.lanza(() -> servicio.verificarUrl("https://otro.sitio.com/foto.png"), ErrorFoto.FOTO_INVALIDA);
        VerificarError.lanza(() -> servicio.verificarUrl(null), ErrorFoto.FOTO_OBLIGATORIA);
        VerificarError.lanza(() -> servicio.verificarUrl("  "), ErrorFoto.FOTO_OBLIGATORIA);
        assertThatCode(() -> servicio.verificarUrl(CONTENEDOR + "dev/buses/abc.png")).doesNotThrowAnyException();
    }
}
