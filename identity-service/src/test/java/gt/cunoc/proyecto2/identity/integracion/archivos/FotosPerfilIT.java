package gt.cunoc.proyecto2.identity.integracion.archivos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.identity.integracion.soporte.BlobFalso;
import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * I10 (subir la foto) e I9 (editar el perfil) contra el Azure Blob de mentira.
 *
 * <p>La foto se sube aparte y el perfil solo guarda su URL, y esa URL tiene que ser del contenedor de
 * fotos: así nadie mete en el perfil una dirección ajena. Cuando se reemplaza, la anterior se borra del
 * almacenamiento, pero solo después de confirmar la transacción y sin interrumpir nada si Azure falla.
 */
class FotosPerfilIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(10);
    private static final String RUTA_SUBIDA = "/identity/archivos/fotos";
    private static final byte[] IMAGEN = {1, 2, 3, 4, 5, 6, 7, 8};

    private ResponseEntity<String> subirFoto(String nombre, String tipoMime) {
        return subirArchivo(RUTA_SUBIDA, tokenAdmin, nombre, tipoMime, IMAGEN);
    }

    /** Un cliente registrado con su token, para editar su propio perfil. */
    private String tokenDeUnCliente(String correo, String nit, String dpi) {
        return tokenCliente(registrarCliente(correo, nit, dpi), correo);
    }

    private ResponseEntity<String> actualizarPerfil(String token, String json) {
        return put("/identity/perfil", token, json);
    }

    private String fotoEnPerfil(String correo) {
        return jdbc.queryForObject("""
                SELECT p.foto_url FROM perfiles p JOIN usuarios u ON u.id = p.usuario_id WHERE u.correo = ?""",
                String.class, correo);
    }

    // ----- I10: subida -----

    @Test
    void subirLaFotoDePerfilLaGuardaEnElContenedorConSuSas() {
        BLOBS.responderPrefijo("PUT", PREFIJO_FOTOS, 201, "");

        ResponseEntity<String> respuesta = subirFoto("yo.jpg", "image/jpeg");

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        String fotoUrl = PruebaIntegracion.json(respuesta, "$.fotoUrl");
        assertThat(fotoUrl).startsWith(CUENTA_FOTOS + PREFIJO_FOTOS + "perfiles/").endsWith(".jpg");

        // Lo que se le mandó a Azure: el blob de esa URL, con el SAS en la consulta y no en una cabecera
        BlobFalso.Peticion puesta = BLOBS.peticiones("PUT").get(0);
        assertThat(CUENTA_FOTOS + puesta.ruta()).isEqualTo(fotoUrl);
        assertThat(puesta.consulta()).isEqualTo(SAS_FOTOS);
        assertThat(puesta.autorizacion()).isNull();
        assertThat(puesta.tipoContenido()).isEqualTo("image/jpeg");
        assertThat(puesta.bytes()).isEqualTo(IMAGEN.length);
    }

    @Test
    void laExtensionSaleDelTipoDeContenidoNoDelNombreDelArchivo() {
        BLOBS.responderPrefijo("PUT", PREFIJO_FOTOS, 201, "");

        // Los dos se llaman .jpg a propósito: lo que manda es el tipo de contenido
        assertThat(PruebaIntegracion.<String>json(subirFoto("yo.jpg", "image/png"), "$.fotoUrl"))
                .endsWith(".png");
        assertThat(PruebaIntegracion.<String>json(subirFoto("yo.jpg", "image/webp"), "$.fotoUrl"))
                .endsWith(".webp");
        assertThat(BLOBS.peticiones("PUT")).hasSize(2);
    }

    @Test
    void unArchivoQueNoEsFotoSeRechazaSinTocarElAlmacenamiento() {
        esError(subirFoto("yo.pdf", "application/pdf"), 400, "ARCHIVO_INVALIDO");
        assertThat(BLOBS.peticiones()).isEmpty();
    }

    @Test
    void siElAlmacenamientoRechazaLaSubidaSeResponde503() {
        // El XML de error de Azure: en el log se recorta y no sale ni el SAS ni el token
        BLOBS.responderPrefijo("PUT", PREFIJO_FOTOS, 403, "<Error><Code>AuthenticationFailed</Code><Message>"
                + "x".repeat(400) + "</Message></Error>");

        esError(subirFoto("yo.jpg", "image/jpeg"), 503, "ALMACENAMIENTO_NO_DISPONIBLE");
        assertThat(BLOBS.peticiones("PUT")).hasSize(1);
    }

    // ----- I9: la foto del perfil -----

    @Test
    void cambiarLaFotoDelPerfilBorraLaAnteriorDespuesDeConfirmar() {
        String token = tokenDeUnCliente("ana@pruebas.gt", "1234567-8", "1234567890101");
        String nueva = CUENTA_FOTOS + PREFIJO_FOTOS + "perfiles/nueva.jpg";
        BLOBS.responder("DELETE", PREFIJO_FOTOS + "perfiles/perfil.jpg", 202, "");

        assertThat(actualizarPerfil(token, "{\"fotoUrl\":\"%s\"}".formatted(FOTO_PERFIL))
                .getStatusCode().value()).isEqualTo(200);
        assertThat(BLOBS.peticiones()).as("sin foto anterior no hay nada que borrar").isEmpty();

        ResponseEntity<String> respuesta = actualizarPerfil(token, "{\"fotoUrl\":\"%s\"}".formatted(nueva));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.fotoUrl")).isEqualTo(nueva);
        await().atMost(ESPERA).untilAsserted(() -> assertThat(BLOBS.peticiones("DELETE")).hasSize(1));
        BlobFalso.Peticion borrado = BLOBS.peticiones("DELETE").get(0);
        assertThat(CUENTA_FOTOS + borrado.ruta()).isEqualTo(FOTO_PERFIL);
        assertThat(borrado.consulta()).isEqualTo(SAS_FOTOS);
        esperarEventos("PerfilActualizado");
    }

    @Test
    void siNoSePuedeBorrarLaFotoAnteriorElPerfilQuedaCambiadoIgual() {
        String token = tokenDeUnCliente("ana@pruebas.gt", "1234567-8", "1234567890101");
        String primera = CUENTA_FOTOS + PREFIJO_FOTOS + "perfiles/primera.jpg";
        String segunda = CUENTA_FOTOS + PREFIJO_FOTOS + "perfiles/segunda.jpg";
        // La vieja ya no estaba (404, que no es error) y la siguiente la rechaza el almacenamiento (500)
        BLOBS.responder("DELETE", PREFIJO_FOTOS + "perfiles/perfil.jpg", 404, "");
        BLOBS.responder("DELETE", PREFIJO_FOTOS + "perfiles/primera.jpg", 500,
                "<Error><Code>InternalError</Code></Error>");

        actualizarPerfil(token, "{\"fotoUrl\":\"%s\"}".formatted(FOTO_PERFIL));
        actualizarPerfil(token, "{\"fotoUrl\":\"%s\"}".formatted(primera));
        actualizarPerfil(token, "{\"fotoUrl\":\"%s\"}".formatted(segunda));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(BLOBS.peticiones("DELETE")).hasSize(2));
        assertThat(fotoEnPerfil("ana@pruebas.gt")).isEqualTo(segunda);
    }

    @Test
    void unaFotoQueNoEsDelContenedorNoEntraAlPerfil() {
        String token = tokenDeUnCliente("ana@pruebas.gt", "1234567-8", "1234567890101");

        esError(actualizarPerfil(token, "{\"fotoUrl\":\"https://otro-sitio.com/fotos/it/perfiles/yo.jpg\"}"),
                400, "FOTO_INVALIDA");
        assertThat(fotoEnPerfil("ana@pruebas.gt")).isNull();
        assertThat(BLOBS.peticiones()).isEmpty();
    }

    @Test
    void unChoferNoSePuedeQuedarSinFoto() {
        UUID usuarioId = registrarCliente("piloto@pruebas.gt", "1234567-8", "1234567890101");
        String token = tokenCliente(usuarioId, "piloto@pruebas.gt");
        // La cuenta queda ligada a un chofer de Fleet: a partir de ahí la foto es obligatoria
        jdbc.update("UPDATE usuarios SET chofer_id = ? WHERE id = ?", UUID.randomUUID(), usuarioId);

        esError(actualizarPerfil(token, "{\"telefono\":\"44443333\"}"), 422, "FOTO_OBLIGATORIA");

        BLOBS.responderPrefijo("PUT", PREFIJO_FOTOS, 201, "");
        String fotoUrl = PruebaIntegracion.json(subirFoto("piloto.jpg", "image/jpeg"), "$.fotoUrl");
        assertThat(actualizarPerfil(token, "{\"fotoUrl\":\"%s\"}".formatted(fotoUrl))
                .getStatusCode().value()).isEqualTo(200);
    }

    // ----- I9: el resto de los campos -----

    @Test
    void actualizarSoloElTelefonoDejaLoDemasComoEstaba() {
        String token = tokenDeUnCliente("ana@pruebas.gt", "1234567-8", "1234567890101");

        ResponseEntity<String> respuesta = actualizarPerfil(token, "{\"telefono\":\"44443333\"}");

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.telefono")).isEqualTo("44443333");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nombres")).isEqualTo("Ana");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.apellidos")).isEqualTo("López");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nit")).isEqualTo("1234567-8");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.direccion")).isEqualTo("Xela");
    }

    @Test
    void elNitSePuedeQuitarConCfOEnBlancoPeroNoRepetirElDeOtro() {
        String ana = tokenDeUnCliente("ana@pruebas.gt", "1234567-8", "1234567890101");
        String beto = tokenDeUnCliente("beto@pruebas.gt", "7654321-9", "1234567890102");

        // El NIT de Ana, que ya está registrado
        esError(actualizarPerfil(beto, "{\"nit\":\"1234567-8\"}"), 409, "NIT_EN_USO");
        // El suyo propio no choca consigo mismo, y uno libre se guarda
        assertThat(PruebaIntegracion.<String>json(actualizarPerfil(beto, "{\"nit\":\"7654321-9\"}"), "$.nit"))
                .isEqualTo("7654321-9");
        assertThat(PruebaIntegracion.<String>json(actualizarPerfil(beto, "{\"nit\":\"9999999-1\"}"), "$.nit"))
                .isEqualTo("9999999-1");
        // "CF" y el vacío son consumidor final: se guarda sin NIT
        assertThat(PruebaIntegracion.<String>json(actualizarPerfil(beto, "{\"nit\":\"cf\"}"), "$.nit")).isNull();
        assertThat(PruebaIntegracion.<String>json(actualizarPerfil(ana, "{\"nit\":\"   \"}"), "$.nit")).isNull();
        assertThat(PruebaIntegracion.<String>json(
                actualizarPerfil(ana, "{\"nombres\":\"Ana María\",\"apellidos\":\"Lopez\",\"direccion\":\"Zona 3\"}"),
                "$.nombres")).isEqualTo("Ana María");
    }
}
