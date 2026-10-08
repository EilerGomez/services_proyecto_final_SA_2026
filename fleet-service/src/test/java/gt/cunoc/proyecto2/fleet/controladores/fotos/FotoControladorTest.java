package gt.cunoc.proyecto2.fleet.controladores.fotos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.dtos.fotos.FotoSubidaRespuesta;
import gt.cunoc.proyecto2.fleet.modelos.fotos.TipoFoto;
import gt.cunoc.proyecto2.fleet.servicios.fotos.AlmacenFotosServicio;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

class FotoControladorTest {

    @Test
    void subirResponde201ConLaUrl() {
        AlmacenFotosServicio fotos = mock(AlmacenFotosServicio.class);
        MockMultipartFile archivo = new MockMultipartFile("archivo", "bus.jpg", "image/jpeg", new byte[] {1});
        when(fotos.subir(TipoFoto.BUS, archivo)).thenReturn(DatosPrueba.FOTO_BUS);

        ResponseEntity<FotoSubidaRespuesta> respuesta = new FotoControlador(fotos).subir(TipoFoto.BUS, archivo);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().fotoUrl()).isEqualTo(DatosPrueba.FOTO_BUS);
    }
}
