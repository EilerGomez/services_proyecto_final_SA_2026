package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.clientes.AdminContacto;
import gt.cunoc.proyecto2.notification.clientes.ClienteIdentity;
import gt.cunoc.proyecto2.notification.evento.recibido.RutaActualizada;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/*
 * Avisa a los administradores de las sucursales de origen y destino cuando cambia una ruta,
 * para que revisen el precio del boleto. Los viajes ya programados no cambian de precio:
 * cada viaje guarda su propio precio al crearse.
 */
@Service
public class CorreoRutasService {

    public static final String RUTA_ACTUALIZADA = "RUTA_ACTUALIZADA";

    private static final int LARGO_ASUNTO = 200;

    private final EnvioCorreoService correos;
    private final ClienteIdentity identity;
    private final JsonMapper json;

    public CorreoRutasService(EnvioCorreoService correos, ClienteIdentity identity, JsonMapper json) {
        this.correos = correos;
        this.identity = identity;
        this.json = json;
    }

    public void rutaActualizada(UUID eventoId, RutaActualizada ruta) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("rutaId", ruta.rutaId());
        datos.put("origen", ruta.origenNombre());
        datos.put("destino", ruta.destinoNombre());
        datos.put("distanciaKm", ruta.distanciaKm());
        datos.put("precioBoleto", ruta.precioBoleto());
        datos.put("activa", ruta.activa());
        String datosJson = json.writeValueAsString(datos);

        String asunto = recortar("Cambió la ruta " + valor(ruta.origenNombre()) + " → " + valor(ruta.destinoNombre()));
        String cuerpo = """
                Hola,

                La ruta %s → %s cambió.

                Distancia actual: %s
                Precio actual del boleto: %s
                Estado: %s

                Revisa el precio del boleto y ajústalo si hace falta.
                Los viajes ya programados conservan el precio con el que se crearon.""".formatted(
                valor(ruta.origenNombre()), valor(ruta.destinoNombre()), kilometros(ruta.distanciaKm()),
                CorreoVentasService.quetzales(ruta.precioBoleto()), ruta.activa() ? "activa" : "inactiva");

        for (AdminContacto admin : administradores(ruta)) {
            correos.enviarPlantilla(eventoId, admin.correo(), RUTA_ACTUALIZADA, asunto, datosJson, cuerpo);
        }
    }

    /** Administradores de origen y destino, sin repetir a quien administra las dos sucursales. */
    private List<AdminContacto> administradores(RutaActualizada ruta) {
        Map<String, AdminContacto> porCorreo = new LinkedHashMap<>();
        Stream.of(ruta.sucursalOrigenId(), ruta.sucursalDestinoId())
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(sucursalId -> identity.administradores(sucursalId).stream())
                .filter(admin -> admin.correo() != null && !admin.correo().isBlank())
                .forEach(admin -> porCorreo.putIfAbsent(admin.correo(), admin));
        return List.copyOf(porCorreo.values());
    }

    private static String kilometros(BigDecimal distancia) {
        return distancia == null ? "-" : distancia.stripTrailingZeros().toPlainString() + " km";
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    private static String recortar(String asunto) {
        return asunto.length() <= LARGO_ASUNTO ? asunto : asunto.substring(0, LARGO_ASUNTO);
    }
}
