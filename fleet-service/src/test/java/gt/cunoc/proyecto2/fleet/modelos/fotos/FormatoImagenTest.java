package gt.cunoc.proyecto2.fleet.modelos.fotos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FormatoImagenTest {

    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8'};

    @Test
    void detectaPorLaFirmaDelArchivo() {
        assertThat(FormatoImagen.detectar(JPG)).contains(FormatoImagen.JPG);
        assertThat(FormatoImagen.detectar(PNG)).contains(FormatoImagen.PNG);
        assertThat(FormatoImagen.detectar(WEBP)).contains(FormatoImagen.WEBP);
    }

    @Test
    void rechazaLoQueNoEsUnaImagenPermitida() {
        byte[] riffSinWebp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};

        assertThat(FormatoImagen.detectar("%PDF-1.7".getBytes())).isEmpty();
        assertThat(FormatoImagen.detectar(riffSinWebp)).isEmpty();
        assertThat(FormatoImagen.detectar(new byte[] {(byte) 0xFF})).isEmpty();
        assertThat(FormatoImagen.detectar(null)).isEmpty();
    }

    @Test
    void tipoMimeYExtension() {
        assertThat(FormatoImagen.JPG.tipoMime()).isEqualTo("image/jpeg");
        assertThat(FormatoImagen.JPG.extension()).isEqualTo("jpg");
        assertThat(FormatoImagen.WEBP.tipoMime()).isEqualTo("image/webp");
        assertThat(TipoFoto.BUS.carpeta()).isEqualTo("buses");
        assertThat(TipoFoto.CHOFER.carpeta()).isEqualTo("choferes");
    }
}
