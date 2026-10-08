package gt.cunoc.proyecto2.fleet.servicios.fotos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.errores.fotos.ErrorFoto;
import gt.cunoc.proyecto2.fleet.modelos.fotos.TipoFoto;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class AlmacenFotosServicioImplTest {

    private static final String BUCKET = "bucket-pruebas";
    private static final String URL_BASE = "https://storage.googleapis.com/" + BUCKET + "/";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    private final Storage storage = mock(Storage.class);
    private AlmacenFotosServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new AlmacenFotosServicioImpl(BUCKET, "dev/");
        // Cliente de GCS simulado: así la prueba no necesita credenciales ni red
        ReflectionTestUtils.setField(servicio, "storage", storage);
    }

    private static MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("archivo", "foto.png", "image/png", contenido);
    }

    // ----- F22 -----

    @Test
    void subeLaFotoConNombreUnicoYDevuelveLaUrlPublica() {
        String url = servicio.subir(TipoFoto.BUS, archivo(PNG));

        ArgumentCaptor<BlobInfo> info = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage).create(info.capture(), eq(PNG));
        assertThat(info.getValue().getBlobId().getBucket()).isEqualTo(BUCKET);
        assertThat(info.getValue().getName()).startsWith("dev/buses/").endsWith(".png");
        assertThat(info.getValue().getContentType()).isEqualTo("image/png");
        assertThat(info.getValue().getCacheControl()).contains("max-age");
        assertThat(url).isEqualTo(URL_BASE + info.getValue().getName());
    }

    @Test
    void rechazaArchivosVaciosGrandesONulos() {
        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, null), ErrorFoto.ARCHIVO_INVALIDO);
        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, archivo(new byte[0])), ErrorFoto.ARCHIVO_INVALIDO);
        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, archivo(new byte[2 * 1024 * 1024 + 1])),
                ErrorFoto.ARCHIVO_INVALIDO);
        verifyNoInteractions(storage);
    }

    @Test
    void rechazaLoQueNoEsImagenAunqueDigaSerPng() {
        VerificarError.lanza(() -> servicio.subir(TipoFoto.CHOFER, archivo("no soy una imagen".getBytes())),
                ErrorFoto.ARCHIVO_INVALIDO);
        verifyNoInteractions(storage);
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
    void siElBucketFallaRespondeServicioNoDisponible() {
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenThrow(new RuntimeException("sin red"));

        VerificarError.lanza(() -> servicio.subir(TipoFoto.BUS, archivo(PNG)), ErrorFoto.ALMACENAMIENTO_NO_DISPONIBLE);
    }


    @Test
    void laUrlDebeSerDeNuestroBucket() {
        VerificarError.lanza(() -> servicio.verificarUrl("https://otro.sitio.com/foto.png"), ErrorFoto.FOTO_INVALIDA);
        assertThatCode(() -> servicio.verificarUrl(URL_BASE + "dev/buses/abc.png")).doesNotThrowAnyException();
    }
}
