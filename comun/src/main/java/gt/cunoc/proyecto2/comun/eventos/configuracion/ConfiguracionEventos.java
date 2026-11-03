package gt.cunoc.proyecto2.comun.eventos.configuracion;

import gt.cunoc.proyecto2.comun.eventos.propiedadesevento.PropiedadesEventos;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

/*
 * Declara en RabbitMQ, al arrancar, la topología del servicio a partir de {PropiedadesEventos}:
 * su exchange, su cola con DLQ y los bindings a los exchanges de otros servicios.
 * Declarar algo que ya existe con los mismos argumentos no tiene efecto.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PropiedadesEventos.class)
public class ConfiguracionEventos {

    @Bean
    public Declarables topologiaEventos(PropiedadesEventos propiedades) {
        List<Declarable> declarables = new ArrayList<>();

        if (StringUtils.hasText(propiedades.exchange())) {
            declarables.add(new TopicExchange(propiedades.exchange(), true, false));// dureable y auto delete
        }

        if (StringUtils.hasText(propiedades.cola())) {
            String dlq = propiedades.cola() + ".dlq";
            declarables.add(QueueBuilder.durable(dlq).build()); //primero la DLQ

            Queue cola = QueueBuilder.durable(propiedades.cola())
                    .deadLetterExchange("")          // exchange por defecto
                    .deadLetterRoutingKey(dlq)       //  <cola>.dlq al agotar reintentos
                    .build();
            declarables.add(cola);

            propiedades.suscripciones().forEach((exchange, eventos) -> {
                TopicExchange origen = new TopicExchange(exchange, true, false);
                declarables.add(origen);
                eventos.forEach(evento -> declarables.add(BindingBuilder.bind(cola).to(origen).with(evento)));
            });
        }
        return new Declarables(declarables);
    }
}
