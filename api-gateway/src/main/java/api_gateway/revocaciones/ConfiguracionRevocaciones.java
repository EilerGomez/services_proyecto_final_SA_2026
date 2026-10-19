package api_gateway.revocaciones;

import java.time.Clock;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Base64UrlNamingStrategy;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Cola propia del gateway para SesionRevocada: anónima (se borra al apagarse, cada instancia recibe
 * todas las revocaciones) y enlazada al exchange de identity. Lo perdido mientras estaba apagado lo
 * recupera {@link CargaInicialRevocaciones} al arrancar.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "gateway.revocaciones", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConfiguracionRevocaciones {

    static final String EVENTO = "SesionRevocada";

    @Bean
    Queue colaRevocaciones() {
        return new AnonymousQueue(new Base64UrlNamingStrategy("api-gateway.revocaciones."));
    }

    /** Misma declaración que hace identity (topic, durable): no importa quién arranque primero. */
    @Bean
    TopicExchange eventosIdentity(@Value("${gateway.revocaciones.exchange:identity.eventos}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }

    @Bean
    Binding enlaceRevocaciones(Queue colaRevocaciones, TopicExchange eventosIdentity) {
        return BindingBuilder.bind(colaRevocaciones).to(eventosIdentity).with(EVENTO);
    }

    @Bean
    TokenServicio tokenServicio(@Value("${app.jwt.secret}") String secreto, ObjectMapper json) {
        return new TokenServicio(secreto, json, Clock.systemUTC());
    }
}
