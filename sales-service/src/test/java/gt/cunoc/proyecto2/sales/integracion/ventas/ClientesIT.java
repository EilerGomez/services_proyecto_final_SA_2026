package gt.cunoc.proyecto2.sales.integracion.ventas;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * S4 · La ventanilla busca al cliente por su NIT antes de vender. Si el NIT es de una cuenta del
 * sistema, la venta en efectivo queda ligada a esa cuenta y el boleto le aparece al pasajero en
 * "Mis boletos", aunque la cuenta se haya creado después de la compra.
 */
class ClientesIT extends PruebaIntegracion {

    private static final String CLIENTES = "/sales/clientes";
    private static final String VENTAS = "/sales/ventas";

    private UUID xela;
    private UUID ana;
    private UUID luis;
    private UUID viaje;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
        ana = UUID.randomUUID();
        luis = UUID.randomUUID();
        insertarCliente(ana, "1234567-K", "Ana López", "ana@correo.com");
        insertarCliente(luis, "7654321-0", "Luis Morales", "luis@correo.com");
        viaje = insertarViaje(xela);
        tripsVendible(viaje);
    }

    // ----- Buscador -----

    @Test
    void sinBusquedaDevuelveLosClientesEnOrdenAlfabetico() {
        ResponseEntity<String> respuesta = get(CLIENTES, tokenCajero(xela));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$[*].nombre"))
                .containsExactly("Ana López", "Luis Morales");
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$[*].usuarioId"))
                .containsExactly(ana.toString(), luis.toString());
    }

    @Test
    void buscaPorNitPorNombreYPorCorreoSinImportarMayusculas() {
        assertThat(PruebaIntegracion.<List<String>>json(
                get(CLIENTES + "?busqueda=7654321", tokenCajero(xela)), "$[*].nombre"))
                .containsExactly("Luis Morales");
        assertThat(PruebaIntegracion.<List<String>>json(
                get(CLIENTES + "?busqueda=ana", tokenCajero(xela)), "$[*].nombre"))
                .containsExactly("Ana López");
        assertThat(PruebaIntegracion.<List<String>>json(
                get(CLIENTES + "?busqueda=LUIS@CORREO", tokenCajero(xela)), "$[*].nombre"))
                .containsExactly("Luis Morales");
    }

    @Test
    void unNitQueNoEsDeNadieNoDevuelveClientes() {
        ResponseEntity<String> respuesta = get(CLIENTES + "?busqueda=9999999-9", tokenCajero(xela));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$[*].nit")).isEmpty();
    }

    @Test
    void elClienteNoPuedeListarALosDemasClientes() {
        esError(get(CLIENTES, tokenCliente(ana)), 403, "ROL_INSUFICIENTE");
    }

    // ----- La venta en efectivo queda ligada a la cuenta -----

    @Test
    void venderConElNitDeUnaCuentaLaLigaYEsaCompraSaleEnMisBoletos() {
        ResponseEntity<String> vendida = post(VENTAS + "/mostrador", tokenCajero(xela), """
                {"viajeId":"%s","asientos":[4,5],"clienteNit":"1234567-k","clienteNombre":null,
                 "clienteCorreo":null,"fechaPago":"%s"}""".formatted(viaje, FECHA_PAGO));

        esStatus(vendida, 201);
        UUID ventaId = UUID.fromString(json(vendida, "$.ventaId"));
        // El NIT en minúsculas es el mismo cliente: se guarda normalizado y con su cuenta
        assertThat(PruebaIntegracion.<String>json(vendida, "$.clienteNit")).isEqualTo("1234567-K");
        assertThat(PruebaIntegracion.<String>json(vendida, "$.clienteNombre")).isEqualTo("Ana López");
        assertThat(jdbc.queryForObject("SELECT cliente_usuario_id FROM ventas WHERE id = ?", UUID.class, ventaId))
                .isEqualTo(ana);
        assertThat(jdbc.queryForObject("SELECT canal FROM ventas WHERE id = ?", String.class, ventaId))
                .isEqualTo("PRESENCIAL");

        // Ana entra a su cuenta y ve el boleto que compró en la ventanilla
        ResponseEntity<String> mio = get("/sales/historial/mio", tokenCliente(ana));
        esStatus(mio, 200);
        assertThat(PruebaIntegracion.<List<String>>json(mio, "$.ventas[*].ventaId"))
                .containsExactly(ventaId.toString());
        assertThat(PruebaIntegracion.<List<String>>json(mio, "$.ventas[*].canal"))
                .containsExactly("PRESENCIAL");
    }

    @Test
    void venderConUnNitDesconocidoNoLigaNingunaCuenta() {
        ResponseEntity<String> vendida = post(VENTAS + "/mostrador", tokenCajero(xela), """
                {"viajeId":"%s","asientos":[6],"clienteNit":"9999999-9","clienteNombre":"Quien sea",
                 "clienteCorreo":null,"fechaPago":"%s"}""".formatted(viaje, FECHA_PAGO));

        esStatus(vendida, 201);
        UUID ventaId = UUID.fromString(json(vendida, "$.ventaId"));
        assertThat(jdbc.queryForObject("SELECT cliente_usuario_id FROM ventas WHERE id = ?", UUID.class, ventaId))
                .isNull();
    }
}
