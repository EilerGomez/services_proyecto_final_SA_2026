package gt.cunoc.proyecto2.notification.models;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;


/**
 * Correo registrado para envío (idempotente por evento + destinatario + plantilla).
 */
@Getter
@Setter
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(name = "canal", length = 10, nullable = false)
    private String canal = "CORREO";

    @Column(name = "destinatario", length = 150, nullable = false)
    private String destinatario;

    @Column(name = "plantilla", length = 50, nullable = false)
    private String plantilla;

    @Column(name = "asunto", length = 200, nullable = false)
    private String asunto;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos", nullable = false)
    private String datos;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "intentos", nullable = false)
    private int intentos = 0;

    @Column(name = "ultimo_error")
    private String ultimoError;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio;
}
