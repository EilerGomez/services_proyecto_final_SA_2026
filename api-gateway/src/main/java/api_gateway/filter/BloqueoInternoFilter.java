package api_gateway.filter;

import api_gateway.constants.ApiPaths;
import api_gateway.exception.ErrorResponseWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Las rutas internas de servicio a servicio (/&lt;prefijo&gt;/interno/**, R1–R5) responden 404
 * desde afuera, antes de la seguridad y del enrutamiento.
 * <p>
 * No compara el texto crudo de la URL: usa los segmentos ya decodificados y sin parámetros de
 * matriz ({@link PathContainer.PathSegment#valueToMatch()}), sin distinguir mayúsculas, y
 * resuelve "." , ".." y "//" como lo hace el servidor del microservicio. Así se bloquean también
 * /fleet/interno;x=1/..., /fleet/%69nterno/..., /fleet/INTERNO/..., /fleet//interno/... y
 * /fleet/x/../interno/....
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class BloqueoInternoFilter implements WebFilter {

    private final ErrorResponseWriter writer;

    public BloqueoInternoFilter(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (esRutaInterna(exchange.getRequest().getPath().pathWithinApplication())) {
            return writer.write(exchange, HttpStatus.NOT_FOUND);
        }
        return chain.filter(exchange);
    }

    /** true si el segundo segmento de la ruta normalizada es "interno" (sin importar mayúsculas). */
    static boolean esRutaInterna(PathContainer ruta) {
        List<String> segmentos = new ArrayList<>();
        for (PathContainer.Element elemento : ruta.elements()) {
            if (elemento instanceof PathContainer.PathSegment segmento) {
                // valueToMatch: decodificado (%69 -> i) y sin ";parametros". Un %2F decodificado
                // se trata como separador.
                for (String parte : segmento.valueToMatch().split("/")) {
                    agregar(segmentos, parte.toLowerCase(Locale.ROOT));
                }
            }
        }
        return segmentos.size() > 1 && ApiPaths.INTERNO.equals(segmentos.get(1));
    }

    private static void agregar(List<String> segmentos, String segmento) {
        if (segmento.isEmpty() || ".".equals(segmento)) {
            return;
        }
        if ("..".equals(segmento)) {
            if (!segmentos.isEmpty()) {
                segmentos.remove(segmentos.size() - 1);
            }
            return;
        }
        segmentos.add(segmento);
    }
}
