package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerAsignado;
import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerPagado;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoVendido;
import gt.cunoc.proyecto2.notification.evento.recibido.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.notification.evento.recibido.VentaCancelada;
import gt.cunoc.proyecto2.notification.evento.recibido.VentaReembolsada;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/*
 * Arma los correos de compras, cancelaciones y alquileres (asunto, datos y cuerpo) y los envía con
 * {@link EnvioCorreoService#enviarPlantilla}, que evita duplicados por evento + destinatario + plantilla.
 */
@Service
public class CorreoVentasService {

    public static final String COMPRA_BOLETO = "COMPRA_BOLETO";
    public static final String VENTA_CANCELADA = "VENTA_CANCELADA";
    public static final String VENTA_REEMBOLSADA = "VENTA_REEMBOLSADA";
    public static final String ALQUILER_PAGADO = "ALQUILER_PAGADO";
    public static final String ALQUILER_ASIGNADO = "ALQUILER_ASIGNADO";
    public static final String COTIZACION_ALQUILER = "COTIZACION_ALQUILER";

    private static final int LARGO_ASUNTO = 200;
    private static final String SIN_DATO = "-";
    private static final String CARTERA = "CARTERA";

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Guatemala");
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a").withZone(ZONA_LOCAL);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EnvioCorreoService correos;
    private final JsonMapper json;

    public CorreoVentasService(EnvioCorreoService correos, JsonMapper json) {
        this.correos = correos;
        this.json = json;
    }

    /* BoletoVendido: comprobante de la compra con viaje, asientos y total. */
    public void compraBoleto(UUID eventoId, BoletoVendido compra) {
        String asunto = "Tu compra " + compra.origenNombre() + " → " + compra.destinoNombre();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("ventaId", compra.ventaId());
        datos.put("origen", compra.origenNombre());
        datos.put("destino", compra.destinoNombre());
        datos.put("fechaSalida", texto(compra.fechaSalida()));
        datos.put("asientos", compra.asientos());
        datos.put("total", compra.montoTotal());
        datos.put("metodoPago", compra.metodoPago());
        String cuerpo = """
                Hola %s,

                Tu compra fue confirmada.

                Viaje: %s → %s
                Salida: %s (hora de Guatemala)
                Bus: %s
                Asientos: %s
                Total: %s, pagado con %s
                Fecha de pago: %s
                Número de venta: %s

                Presenta este correo al abordar. ¡Buen viaje!""".formatted(
                compra.clienteNombre(), compra.origenNombre(), compra.destinoNombre(),
                fechaHora(compra.fechaSalida()), valor(compra.busPlaca()), asientos(compra.asientos()),
                quetzales(compra.montoTotal()), metodo(compra.metodoPago()), fecha(compra.fechaPago()),
                compra.ventaId());
        correos.enviarPlantilla(eventoId, compra.clienteCorreo(), COMPRA_BOLETO, recortar(asunto),
                json.writeValueAsString(datos), cuerpo);
    }

    /* VentaCancelada: avisa la anulación y, si pagó con cartera, que el monto vuelve a su cartera. */
    public void ventaCancelada(UUID eventoId, VentaCancelada venta) {
        boolean reembolsoACartera = CARTERA.equals(venta.metodoPago());
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("ventaId", venta.ventaId());
        datos.put("asientos", venta.asientos());
        datos.put("monto", venta.montoAnulado());
        datos.put("motivo", venta.motivo());
        datos.put("reembolsoACartera", reembolsoACartera);
        String viaje = venta.origenNombre() == null
                ? ""
                : "Viaje: %s → %s, salida %s (hora de Guatemala)%n".formatted(
                        venta.origenNombre(), venta.destinoNombre(), fechaHora(venta.fechaSalida()));
        String devolucion = reembolsoACartera
                ? "El monto se devolverá a tu cartera digital en unos momentos."
                : "Acércate a la sucursal para la devolución del efectivo.";
        String cuerpo = """
                Hola %s,

                Tu compra %s fue cancelada el %s.

                %sAsientos: %s
                Monto anulado: %s
                Motivo: %s

                %s""".formatted(
                venta.clienteNombre(), venta.ventaId(), fecha(venta.fechaCancelacion()), viaje,
                asientos(venta.asientos()), quetzales(venta.montoAnulado()), valor(venta.motivo()), devolucion);
        correos.enviarPlantilla(eventoId, venta.clienteCorreo(), VENTA_CANCELADA, "Tu compra fue cancelada",
                json.writeValueAsString(datos), cuerpo);
    }

    /* VentaReembolsada: la sucursal ya devolvió el efectivo de una compra cancelada. */
    public void ventaReembolsada(UUID eventoId, VentaReembolsada venta) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("ventaId", venta.ventaId());
        datos.put("codigo", venta.codigo());
        datos.put("asientos", venta.asientos());
        datos.put("monto", venta.montoReembolsado());
        datos.put("motivo", venta.motivo());
        datos.put("fechaReembolso", texto(venta.fechaReembolso()));
        String viaje = venta.origenNombre() == null
                ? ""
                : "Viaje: %s → %s, salida %s (hora de Guatemala)%n".formatted(
                        venta.origenNombre(), venta.destinoNombre(), fechaHora(venta.fechaSalida()));
        String cuerpo = """
                Hola %s,

                Ya te devolvimos el dinero de tu compra %s, cancelada antes de viajar.

                %sAsientos: %s
                Monto devuelto: %s, en %s
                Fecha de la devolución: %s
                Motivo de la cancelación: %s

                Con esto la compra queda cerrada. Si algo no cuadra, acércate a la sucursal
                con tu boleto impreso y el código %s.""".formatted(
                venta.clienteNombre(), venta.ventaId(), viaje, asientos(venta.asientos()),
                quetzales(venta.montoReembolsado()), metodo(venta.metodoPago()),
                fecha(venta.fechaReembolso()), valor(venta.motivo()), valor(venta.codigo()));
        correos.enviarPlantilla(eventoId, venta.clienteCorreo(), VENTA_REEMBOLSADA,
                "Devolución de tu compra cancelada", json.writeValueAsString(datos), cuerpo);
    }

    /* AlquilerPagado: confirma el pago del viaje privado al cliente. La sucursal no recibe correo:
       el aviso de "pagos por asignar" se ve en el menú del panel. */
    public void alquilerPagado(UUID eventoId, AlquilerPagado alquiler) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("ordenId", alquiler.ordenId());
        datos.put("alquilerId", alquiler.alquilerId());
        datos.put("origen", alquiler.origen());
        datos.put("destino", alquiler.destino());
        datos.put("monto", alquiler.monto());
        datos.put("metodoPago", alquiler.metodoPago());
        datos.put("fechaPago", texto(alquiler.fechaPago()));
        String cuerpo = """
                Hola %s,

                Recibimos el pago de tu alquiler.

                Recorrido: %s → %s
                Monto: %s, pagado con %s
                Fecha de pago: %s

                Te avisaremos cuando el viaje tenga bus y piloto asignados.""".formatted(
                alquiler.clienteNombre(), alquiler.origen(), alquiler.destino(), quetzales(alquiler.monto()),
                metodo(alquiler.metodoPago()), fecha(alquiler.fechaPago()));
        correos.enviarPlantilla(eventoId, alquiler.clienteCorreo(), ALQUILER_PAGADO, "Pago de alquiler recibido",
                json.writeValueAsString(datos), cuerpo);
    }

    /* AlquilerAsignado: el viaje privado ya tiene bus y piloto. */
    public void alquilerAsignado(UUID eventoId, AlquilerAsignado asignado) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("alquilerId", asignado.alquilerId());
        datos.put("viajeId", asignado.viajeId());
        datos.put("origen", asignado.origen());
        datos.put("destino", asignado.destino());
        datos.put("fechaSalida", texto(asignado.fechaSalida()));
        datos.put("fechaRetorno", texto(asignado.fechaRetorno()));
        datos.put("busPlaca", asignado.busPlaca());
        datos.put("choferNombre", asignado.choferNombre());
        String cuerpo = """
                Hola %s,

                Tu viaje privado ya tiene bus y piloto asignados.

                Recorrido: %s → %s
                Salida: %s (hora de Guatemala)
                Retorno: %s
                Bus: %s
                Piloto: %s

                ¡Buen viaje!""".formatted(
                asignado.clienteNombre(), asignado.origen(), asignado.destino(),
                fechaHora(asignado.fechaSalida()), fechaHora(asignado.fechaRetorno()),
                valor(asignado.busPlaca()), valor(asignado.choferNombre()));
        correos.enviarPlantilla(eventoId, asignado.clienteCorreo(), ALQUILER_ASIGNADO,
                "Tu alquiler ya tiene bus y piloto", json.writeValueAsString(datos), cuerpo);
    }

    /* CotizacionAlquilerConfirmada: precio final del alquiler y cómo pagarlo. */
    public void cotizacionAlquiler(UUID eventoId, CotizacionAlquilerConfirmada cotizacion) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("alquilerId", cotizacion.alquilerId());
        datos.put("origen", cotizacion.origen());
        datos.put("destino", cotizacion.destino());
        datos.put("fechaSalida", texto(cotizacion.fechaSalida()));
        datos.put("fechaRetorno", texto(cotizacion.fechaRetorno()));
        datos.put("pasajeros", cotizacion.cantidadPasajeros());
        datos.put("precio", cotizacion.precioConfirmado());
        String cuerpo = """
                Hola %s,

                Tu cotización de alquiler está lista.

                Recorrido: %s → %s
                Salida: %s (hora de Guatemala)
                Retorno: %s
                Pasajeros: %d
                Precio: %s

                Puedes pagarla en línea con tu cartera digital (si tienes cuenta)
                o en efectivo en la sucursal.""".formatted(
                cotizacion.clienteNombre(), cotizacion.origen(), cotizacion.destino(),
                fechaHora(cotizacion.fechaSalida()), fechaHora(cotizacion.fechaRetorno()),
                cotizacion.cantidadPasajeros(), quetzales(cotizacion.precioConfirmado()));
        correos.enviarPlantilla(eventoId, cotizacion.clienteCorreo(), COTIZACION_ALQUILER,
                "Tu cotización de alquiler está lista", json.writeValueAsString(datos), cuerpo);
    }

    // ----- Formato -----

    static String quetzales(BigDecimal monto) {
        return monto == null ? SIN_DATO : "Q " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String metodo(String metodoPago) {
        return CARTERA.equals(metodoPago) ? "cartera digital" : "efectivo";
    }

    static String asientos(List<Integer> asientos) {
        return asientos == null || asientos.isEmpty()
                ? SIN_DATO
                : asientos.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    static String fechaHora(OffsetDateTime fecha) {
        return fecha == null ? SIN_DATO : FECHA_HORA.format(fecha);
    }

    static String fecha(LocalDate fecha) {
        return fecha == null ? SIN_DATO : FECHA.format(fecha);
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? SIN_DATO : texto;
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static String recortar(String asunto) {
        return asunto.length() <= LARGO_ASUNTO ? asunto : asunto.substring(0, LARGO_ASUNTO);
    }
}