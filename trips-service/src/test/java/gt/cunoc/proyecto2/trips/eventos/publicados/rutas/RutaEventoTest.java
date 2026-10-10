package gt.cunoc.proyecto2.trips.eventos.publicados.rutas;

import static gt.cunoc.proyecto2.trips.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.dtos.rutas.RutaRespuesta;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RutaEventoTest {

    @Test
    void desdeLaRespuestaCopiaIdsNombresYPrecio() {
        RutaEntidad ruta = DatosPrueba.ruta();
        RutaRespuesta respuesta = new RutaRespuesta(ruta, Map.of(XELA, "Xela", GUATE, "Guate"));

        RutaEvento evento = RutaEvento.desde(respuesta);

        assertThat(evento.rutaId()).isEqualTo(ruta.getId());
        assertThat(evento.sucursalOrigenId()).isEqualTo(XELA);
        assertThat(evento.sucursalDestinoId()).isEqualTo(GUATE);
        assertThat(evento.origenNombre()).isEqualTo("Xela");
        assertThat(evento.destinoNombre()).isEqualTo("Guate");
        assertThat(evento.distanciaKm()).isEqualByComparingTo("205.00");
        assertThat(evento.precioBoleto()).isEqualByComparingTo("75.00");
        assertThat(evento.activa()).isTrue();
    }

    @Test
    void sinNombreDeSucursalElEventoLlevaNulo() {
        RutaEntidad ruta = DatosPrueba.ruta();
        RutaRespuesta respuesta = new RutaRespuesta(ruta, Map.of(XELA, "Xela"));

        RutaEvento evento = RutaEvento.desde(respuesta);

        assertThat(evento.origenNombre()).isEqualTo("Xela");
        assertThat(evento.destinoNombre()).isNull();
    }

    @Test
    void rutaEliminadaSoloLlevaElId() {
        UUID rutaId = UUID.randomUUID();

        assertThat(new RutaEliminada(rutaId).rutaId()).isEqualTo(rutaId);
        assertThat(new RutaEliminada(rutaId)).isEqualTo(new RutaEliminada(rutaId));
    }
}
