package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.clientes.ClienteIdentity;
import gt.cunoc.proyecto2.notification.clientes.ContactoUsuario;
import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerAsignado;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoReasignado;
import gt.cunoc.proyecto2.notification.evento.recibido.ViajeProgramado;
import gt.cunoc.proyecto2.notification.evento.recibido.ViajeReasignado;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/*
 * Avisos de cambios en un viaje ya programado. Hoy solo uno: le cambiaron el bus, el piloto o los
 * dos, y quien va a viajar tiene que enterarse antes de buscar la unidad equivocada en la terminal.
 *
 * Llegan por dos caminos porque el dato del destinatario vive en servicios distintos: el pasajero
 * con boleto lo sabe Sales (BoletoReasignado) y el cliente de un alquiler privado lo sabe Trips
 * (ViajeReasignado, que trae su correo).
 */
@Service
public class CorreoViajesService {

    public static final String VIAJE_REASIGNADO = "VIAJE_REASIGNADO";
    /** Aviso al piloto de que le tocó un viaje (regular o un alquiler privado). */
    public static final String VIAJE_ASIGNADO_PILOTO = "VIAJE_ASIGNADO_PILOTO";

    private static final int LARGO_ASUNTO = 200;
    private static final String SIN_DATO = "-";

    private final EnvioCorreoService correos;
    private final ClienteIdentity identity;
    private final JsonMapper json;

    public CorreoViajesService(EnvioCorreoService correos, ClienteIdentity identity, JsonMapper json) {
        this.correos = correos;
        this.identity = identity;
        this.json = json;
    }

    /*
     * ViajeProgramado (Trips): al piloto se le avisa que tiene un viaje nuevo, con el recorrido, la
     * unidad y la hora a la que sale. El correo no viene en el evento (Trips no guarda correos): se
     * pide a Identity con la cuenta del piloto, igual que el recordatorio de licencia.
     */
    public void viajeAsignadoAlPiloto(UUID eventoId, ViajeProgramado viaje) {
        Optional<ContactoUsuario> piloto = contactoDelPiloto(viaje.choferUsuarioId());
        if (piloto.isEmpty()) {
            return;
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("viajeId", texto(viaje.viajeId()));
        datos.put("tipo", viaje.tipo());
        datos.put("sentido", viaje.sentido());
        datos.put("busPlaca", viaje.busPlaca());
        datos.put("origenNombre", viaje.origenNombre());
        datos.put("destinoNombre", viaje.destinoNombre());

        String recorrido = valor(viaje.origenNombre()) + " → " + valor(viaje.destinoNombre());
        String cuerpo = """
                Hola %s:

                Se te asignó un viaje nuevo.

                %s: %s
                Sale: %s
                Llegada estimada: %s
                Bus: %s

                Recuerda registrar la salida al arrancar y la llegada al terminar, desde Mis viajes.

                ¡Buen viaje!""".formatted(
                valor(piloto.get().nombre()),
                claseDeViaje(viaje), recorrido,
                CorreoVentasService.fechaHora(viaje.fechaSalida()),
                CorreoVentasService.fechaHora(viaje.fechaLlegadaEstimada()),
                valor(viaje.busPlaca()));

        correos.enviarPlantilla(eventoId, piloto.get().correo(), VIAJE_ASIGNADO_PILOTO,
                recortar("Viaje asignado: " + recorrido), json.writeValueAsString(datos), cuerpo);
    }

    /*
     * AlquilerAsignado (Trips): el mismo aviso, pero para el piloto del alquiler privado. Va aparte
     * del correo del cliente (ese lo manda CorreoVentasService) porque son dos destinatarios.
     */
    public void alquilerAsignadoAlPiloto(UUID eventoId, AlquilerAsignado alquiler) {
        Optional<ContactoUsuario> piloto = contactoDelPiloto(alquiler.choferUsuarioId());
        if (piloto.isEmpty()) {
            return;
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("alquilerId", texto(alquiler.alquilerId()));
        datos.put("viajeId", texto(alquiler.viajeId()));
        datos.put("busPlaca", alquiler.busPlaca());
        datos.put("origen", alquiler.origen());
        datos.put("destino", alquiler.destino());

        String recorrido = valor(alquiler.origen()) + " → " + valor(alquiler.destino());
        String cuerpo = """
                Hola %s:

                Se te asignó un alquiler privado.

                Recorrido: %s
                Sale: %s
                Retorno: %s
                Bus: %s
                Cliente: %s

                Recuerda registrar la salida al arrancar y la llegada al terminar, desde Mis viajes.

                ¡Buen viaje!""".formatted(
                valor(piloto.get().nombre()),
                recorrido,
                CorreoVentasService.fechaHora(alquiler.fechaSalida()),
                alquiler.fechaRetorno() == null ? "solo ida"
                        : CorreoVentasService.fechaHora(alquiler.fechaRetorno()),
                valor(alquiler.busPlaca()),
                valor(alquiler.clienteNombre()));

        correos.enviarPlantilla(eventoId, piloto.get().correo(), VIAJE_ASIGNADO_PILOTO,
                recortar("Alquiler privado asignado: " + recorrido), json.writeValueAsString(datos), cuerpo);
    }

    /** El contacto del piloto en Identity; vacío si el evento no trae su cuenta o si ya no existe. */
    private Optional<ContactoUsuario> contactoDelPiloto(UUID usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }
        return identity.contacto(usuarioId)
                .filter(contacto -> contacto.correo() != null && !contacto.correo().isBlank());
    }

    /** Cómo nombrarle el viaje al piloto: el de vuelta se avisa como tal para no confundirlo. */
    private static String claseDeViaje(ViajeProgramado viaje) {
        if (viaje.esPrivado()) {
            return "Alquiler privado";
        }
        return viaje.esRegreso() ? "Viaje de regreso" : "Viaje";
    }

    /* BoletoReasignado (Sales): al pasajero de un viaje regular le cambiaron el bus o el piloto. */
    public void boletoReasignado(UUID eventoId, BoletoReasignado boleto) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("ventaId", texto(boleto.ventaId()));
        datos.put("codigo", boleto.codigo());
        datos.put("viajeId", texto(boleto.viajeId()));
        datos.put("asientos", boleto.asientos());
        datos.put("cambioBus", boleto.cambioBus());
        datos.put("cambioChofer", boleto.cambioChofer());
        datos.put("busPlaca", boleto.busPlaca());
        datos.put("choferNombre", boleto.choferNombre());

        String cuerpo = """
                Hola %s:

                Hubo un cambio en tu viaje %s → %s del %s.

                %s

                Tus asientos siguen siendo los mismos: %s. Tu boleto no cambia y no tienes que hacer
                nada: solo busca la placa nueva al abordar.

                Disculpa la molestia. ¡Buen viaje!""".formatted(
                valor(boleto.clienteNombre()),
                valor(boleto.origenNombre()), valor(boleto.destinoNombre()),
                CorreoVentasService.fechaHora(boleto.fechaSalida()),
                detalleDelCambio(boleto.cambioBus(), boleto.busPlacaAnterior(), boleto.busPlaca(),
                        boleto.cambioChofer(), boleto.choferAnterior(), boleto.choferNombre()),
                asientos(boleto.asientos()));

        correos.enviarPlantilla(eventoId, boleto.clienteCorreo(), VIAJE_REASIGNADO,
                recortar(asunto(boleto.cambioBus(), boleto.cambioChofer(), boleto.origenNombre(),
                        boleto.destinoNombre())),
                json.writeValueAsString(datos), cuerpo);
    }

    /*
     * ViajeReasignado (Trips): al cliente de un alquiler privado le cambiaron el bus o el piloto.
     * Un viaje regular llega sin correo (sus pasajeros los avisa Sales) y entonces no se escribe.
     */
    public void viajeReasignado(UUID eventoId, ViajeReasignado viaje) {
        if (viaje.clienteCorreo() == null || viaje.clienteCorreo().isBlank()) {
            return;
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("viajeId", texto(viaje.viajeId()));
        datos.put("tipo", viaje.tipo());
        datos.put("cambioBus", viaje.cambioBus());
        datos.put("cambioChofer", viaje.cambioChofer());
        datos.put("busPlaca", viaje.busPlaca());
        datos.put("choferNombre", viaje.choferNombre());

        String cuerpo = """
                Hola %s:

                Hubo un cambio en el bus que contrataste para tu viaje %s → %s del %s.

                %s

                Las fechas, el recorrido y el precio de tu alquiler no cambian.

                Disculpa la molestia. ¡Buen viaje!""".formatted(
                valor(viaje.clienteNombre()),
                valor(viaje.origenNombre()), valor(viaje.destinoNombre()),
                CorreoVentasService.fechaHora(viaje.fechaSalida()),
                detalleDelCambio(viaje.cambioBus(), viaje.busPlacaAnterior(), viaje.busPlaca(),
                        viaje.cambioChofer(), viaje.choferAnterior(), viaje.choferNombre()));

        correos.enviarPlantilla(eventoId, viaje.clienteCorreo(), VIAJE_REASIGNADO,
                recortar(asunto(viaje.cambioBus(), viaje.cambioChofer(), viaje.origenNombre(),
                        viaje.destinoNombre())),
                json.writeValueAsString(datos), cuerpo);
    }

    /** El asunto dice de una vez qué cambió, para que se entienda sin abrir el correo. */
    private static String asunto(boolean cambioBus, boolean cambioChofer, String origen, String destino) {
        String que = cambioBus && cambioChofer ? "Cambio de bus y de piloto"
                : cambioBus ? "Cambio de bus" : "Cambio de piloto";
        return que + ": " + valor(origen) + " → " + valor(destino);
    }

    /** Las dos o tres líneas del cuerpo que dicen qué unidad y qué piloto quedan a cargo. */
    private static String detalleDelCambio(boolean cambioBus, String busAnterior, String busPlaca,
                                           boolean cambioChofer, String choferAnterior, String choferNombre) {
        StringBuilder detalle = new StringBuilder();
        if (cambioBus) {
            detalle.append("El bus asignado ahora es el ").append(valor(busPlaca))
                    .append(" (antes era el ").append(valor(busAnterior)).append(").");
        }
        if (cambioChofer) {
            if (!detalle.isEmpty()) {
                detalle.append(System.lineSeparator());
            }
            detalle.append("El piloto a cargo ahora es ").append(valor(choferNombre))
                    .append(" (antes era ").append(valor(choferAnterior)).append(").");
        }
        return detalle.toString();
    }

    private static String asientos(List<Integer> asientos) {
        return asientos == null || asientos.isEmpty()
                ? SIN_DATO
                : asientos.stream().map(String::valueOf).collect(Collectors.joining(", "));
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
