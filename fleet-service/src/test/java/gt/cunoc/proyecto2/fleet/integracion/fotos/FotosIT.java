package gt.cunoc.proyecto2.fleet.integracion.fotos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.fleet.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.fleet.integracion.soporte.ServidorFalso;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * F22 · subida de fotos contra el almacenamiento, con el Azure Blob de mentira.
 *
 * <p>El formato no se cree del Content-Type que manda el navegador: se detecta por los primeros bytes
 * del archivo. El nombre del blob es un UUID, así que la URL no se puede adivinar.
 *
 * <p>También se ve el borrado de la foto anterior cuando a un bus le cambian la foto: sale después de
 * confirmar la transacción y, si el almacenamiento lo rechaza, el cambio del bus queda hecho igual.
 */
class FotosIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(10);

    /** Prefijo de todos los blobs de esta prueba: app.fotos.contenedor + app.fotos.prefijo. */
    private static final String PREFIJO_BLOB = "/fotos/it/";

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 32, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    private UUID xela;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("XELA", true);
    }

    private ResponseEntity<String> subirFoto(String tipo, String nombre, byte[] contenido) {
        return subirArchivo("/fleet/archivos/fotos?tipo=" + tipo, tokenAdminSucursal(xela), nombre, contenido);
    }

    private String actualizarBus(UUID bus, String fotoUrl, int version) {
        ResponseEntity<String> respuesta = put("/fleet/buses/" + bus, tokenAdminSucursal(xela), """
                {"marca":"Scania","modelo":"K410","anioFabricacion":2022,"capacidadPasajeros":55,
                 "fotoUrl":"%s","version":%d}""".formatted(fotoUrl, version));
        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(200);
        return respuesta.getBody();
    }

    private void esperarBorrados(int cuantos) {
        await().atMost(ESPERA).untilAsserted(() -> assertThat(BLOBS.peticiones("DELETE")).hasSize(cuantos));
    }

    // ----- Subida -----

    @Test
    void subirUnJpgLoGuardaEnElContenedorYDevuelveSuUrl() {
        BLOBS.responderPrefijo("PUT", PREFIJO_BLOB, 201, "");

        ResponseEntity<String> respuesta = subirFoto("BUS", "bus.jpg", JPG);

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        String fotoUrl = PruebaIntegracion.json(respuesta, "$.fotoUrl");
        assertThat(fotoUrl).startsWith(CUENTA_FOTOS + PREFIJO_BLOB + "buses/").endsWith(".jpg");

        // Lo que se le mandó a Azure: el blob de la URL, con el SAS en la consulta (no en una cabecera)
        ServidorFalso.Peticion puesta = BLOBS.peticiones("PUT").get(0);
        assertThat(CUENTA_FOTOS + puesta.ruta()).isEqualTo(fotoUrl);
        assertThat(puesta.consulta()).isEqualTo(SAS_FOTOS);
        assertThat(puesta.autorizacion()).isNull();
    }

    @Test
    void elPngYElWebpSeReconocenPorSusPrimerosBytesNoPorElNombre() {
        BLOBS.responderPrefijo("PUT", PREFIJO_BLOB, 201, "");

        // Los dos van con nombre .jpg a propósito: el nombre no decide nada
        assertThat(PruebaIntegracion.<String>json(subirFoto("CHOFER", "chofer.jpg", PNG), "$.fotoUrl"))
                .startsWith(CUENTA_FOTOS + PREFIJO_BLOB + "choferes/").endsWith(".png");
        assertThat(PruebaIntegracion.<String>json(subirFoto("BUS", "bus.jpg", WEBP), "$.fotoUrl"))
                .endsWith(".webp");
        assertThat(BLOBS.peticiones("PUT")).hasSize(2);
    }

    @Test
    void siElAlmacenamientoRechazaLaSubidaSeResponde503() {
        // El XML de error de Azure: en el log se recorta, y no sale ni el SAS ni el token
        BLOBS.responderPrefijo("PUT", PREFIJO_BLOB, 403, "<Error><Code>AuthenticationFailed</Code><Message>"
                + "x".repeat(400) + "</Message></Error>");

        esError(subirFoto("BUS", "bus.jpg", JPG), 503, "ALMACENAMIENTO_NO_DISPONIBLE");
        assertThat(BLOBS.peticiones("PUT")).hasSize(1);
    }

    // ----- Borrado de la foto anterior (F13) -----

    @Test
    void alCambiarLaFotoDelBusSeBorraLaAnteriorDelAlmacenamiento() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "0");   // queda con FOTO_BUS
        BLOBS.responder("DELETE", PREFIJO_BLOB + "buses/bus.jpg", 202, "");

        actualizarBus(bus, CUENTA_FOTOS + PREFIJO_BLOB + "buses/nueva.jpg", 0);

        esperarBorrados(1);
        ServidorFalso.Peticion borrado = BLOBS.peticiones("DELETE").get(0);
        assertThat(CUENTA_FOTOS + borrado.ruta()).isEqualTo(FOTO_BUS);
        assertThat(borrado.consulta()).isEqualTo(SAS_FOTOS);
        esperarEventos("BusActualizado");
    }

    @Test
    void siNoSePuedeBorrarLaFotoAnteriorElCambioDelBusQuedaHechoIgual() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "0");
        String primera = CUENTA_FOTOS + PREFIJO_BLOB + "buses/primera.jpg";
        String segunda = CUENTA_FOTOS + PREFIJO_BLOB + "buses/segunda.jpg";
        // La vieja ya no estaba (404, que no es error) y la siguiente la rechaza el almacenamiento (500)
        BLOBS.responder("DELETE", PREFIJO_BLOB + "buses/bus.jpg", 404, "");
        BLOBS.responder("DELETE", PREFIJO_BLOB + "buses/primera.jpg", 500, "<Error><Code>InternalError</Code></Error>");

        actualizarBus(bus, primera, 0);
        actualizarBus(bus, segunda, 1);

        esperarBorrados(2);
        assertThat(jdbc.queryForObject("SELECT foto_url FROM buses WHERE id = ?", String.class, bus))
                .isEqualTo(segunda);
    }
}
