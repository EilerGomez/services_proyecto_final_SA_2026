package gt.cunoc.proyecto2.notification.services.correo;


import gt.cunoc.proyecto2.notification.models.Notificacion;
import gt.cunoc.proyecto2.notification.repositories.NotificacionRepository;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;


/**
 * Registro idempotente y envío de correos (Mailpit en dev, SMTP real en prod).
 *
 * <p>Si el envío falla se lanza la excepción para que comun reintente y luego DLQ.
 */
@Service
public class EnvioCorreoService {

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Guatemala");

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a").withZone(ZONA_LOCAL);

    private final NotificacionRepository notificaciones;

    private final JavaMailSender correos;

    private final String remitente;

    public EnvioCorreoService(
            NotificacionRepository notificaciones,
            JavaMailSender correos,
            @Value("${app.correos.remitente:notificaciones@proyecto2.local}") String remitente) {
        this.notificaciones = notificaciones;
        this.correos = correos;
        this.remitente = remitente;
    }

    @Transactional
    public void enviarRecuperacion(UUID eventoId, String correo, String nombre, String url, Instant expiraEn) {
        Optional<Notificacion> previa = registrada(eventoId, correo, "RECUPERACION_CONTRASENA",
                "Recupera tu contraseña",
                "{\"url\":\"" + url + "\",\"expiraEn\":\"" + expiraEn + "\"}");
        if (previa.isEmpty()) {
            return;
        }
        String cuerpo = "Hola " + nombre + ",\n\nSolicitaste recuperar tu contraseña.\n\n"
                + "Usa este enlace antes del " + FORMATO_FECHA.format(expiraEn) + " (hora de Guatemala):\n" + url + "\n\n"
                + "Si no fuiste tú, ignora este correo.";
        enviar(previa.get(), correo, "Recupera tu contraseña", cuerpo, null);
    }

    @Transactional
    public void enviarBienvenida(UUID eventoId, String correo, String nombre, String urlActivacion) {
        Optional<Notificacion> previa = registrada(eventoId, correo, "CUENTA_CREADA",
                "Bienvenido a la plataforma",
                "{\"urlActivacion\":" + (urlActivacion == null ? "null" : "\"" + urlActivacion + "\"") + "}");
        if (previa.isEmpty()) {
            return;
        }
        String cuerpo = "Hola " + nombre + ",\n\nTu cuenta fue creada con éxito.\n"
                + (urlActivacion == null
                        ? "\nYa puedes iniciar sesión."
                        : "\nActívala aquí:\n" + urlActivacion + "\n");
        enviar(previa.get(), correo, "Bienvenido a la plataforma", cuerpo, null);
    }

    @Transactional
    public void enviarCodigoDobleFactor(
            UUID eventoId, String correo, String nombre, String codigo, Instant expiraEn) {
        Optional<Notificacion> previa = registrada(eventoId, correo, "CODIGO_DOBLE_FACTOR",
                "Tu código de verificación",
                "{\"expiraEn\":\"" + expiraEn + "\"}");
        if (previa.isEmpty()) {
            return;
        }
        String cuerpo = "Hola " + nombre + ",\n\nTu código de verificación es:\n\n"
                + codigo + "\n\nVence el " + FORMATO_FECHA.format(expiraEn) + " (hora de Guatemala).\n"
                + "Si no fuiste tú, ignora este correo.";
        enviar(previa.get(), correo, "Tu código de verificación", cuerpo, null);
    }

    /*
     * Correo con asunto, datos y cuerpo ya armados (compras, cancelaciones, alquileres y cotizaciones).
     * Mismo registro idempotente que los demás. Sin correo (venta en mostrador sin correo) no hace nada.
     */
    @Transactional
    public void enviarPlantilla(
            UUID eventoId, String correo, String plantilla, String asunto, String datos, String cuerpo) {
        enviarPlantilla(eventoId, correo, plantilla, asunto, datos, cuerpo, null);
    }

    /*
     * Igual que el anterior, pero con un archivo adjunto (el PDF del boleto en las compras).
     * Si no hay adjunto se manda texto plano, como siempre.
     */
    @Transactional
    public void enviarPlantilla(UUID eventoId, String correo, String plantilla, String asunto, String datos,
                                String cuerpo, AdjuntoCorreo adjunto) {
        if (correo == null || correo.isBlank()) {
            return;
        }
        Optional<Notificacion> previa = registrada(eventoId, correo, plantilla, asunto, datos);
        if (previa.isEmpty()) {
            return;
        }
        enviar(previa.get(), correo, asunto, cuerpo, adjunto);
    }

    private Optional<Notificacion> registrada(
            UUID eventoId, String correo, String plantilla, String asunto, String datos) {
        Optional<Notificacion> existente =
                notificaciones.findByEventoIdAndDestinatarioAndPlantilla(eventoId, correo, plantilla);
        if (existente.isPresent()) {
            return existente.get().getEstado().equals("ENVIADA") ? Optional.empty() : Optional.of(existente.get());
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setEventoId(eventoId);
        notificacion.setDestinatario(correo);
        notificacion.setPlantilla(plantilla);
        notificacion.setAsunto(asunto);
        notificacion.setDatos(datos);
        try {
            notificaciones.saveAndFlush(notificacion);
        } catch (DataIntegrityViolationException duplicada) {
            return Optional.empty();
        }
        return Optional.of(notificacion);
    }

    private void enviar(Notificacion notificacion, String destino, String asunto, String cuerpo,
                        AdjuntoCorreo adjunto) {
        notificacion.setIntentos(notificacion.getIntentos() + 1);
        try {
            if (adjunto == null) {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setFrom(remitente);
                mensaje.setTo(destino);
                mensaje.setSubject(asunto);
                mensaje.setText(cuerpo);
                correos.send(mensaje);
            } else {
                correos.send(conAdjunto(destino, asunto, cuerpo, adjunto));
            }
        } catch (MailException falla) {
            marcarFallida(notificacion, falla);
            throw falla;
        } catch (MessagingException falla) {
            MailSendException error = new MailSendException("No se pudo armar el correo con adjunto", falla);
            marcarFallida(notificacion, error);
            throw error;
        }
        notificacion.setEstado("ENVIADA");
        notificacion.setFechaEnvio(Instant.now());
        notificaciones.save(notificacion);
    }

    /** Correo multiparte: el cuerpo de siempre más el archivo adjunto. */
    private MimeMessage conAdjunto(String destino, String asunto, String cuerpo, AdjuntoCorreo adjunto)
            throws MessagingException {
        MimeMessage mensaje = correos.createMimeMessage();
        mensaje.setFrom(new InternetAddress(remitente));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
        mensaje.setSubject(asunto, StandardCharsets.UTF_8.name());

        MimeBodyPart texto = new MimeBodyPart();
        texto.setText(cuerpo, StandardCharsets.UTF_8.name());

        MimeBodyPart archivo = new MimeBodyPart();
        archivo.setDataHandler(new DataHandler(
                new ByteArrayDataSource(adjunto.contenido(), adjunto.tipoContenido())));
        archivo.setFileName(adjunto.nombre());

        Multipart partes = new MimeMultipart();
        partes.addBodyPart(texto);
        partes.addBodyPart(archivo);
        mensaje.setContent(partes);
        return mensaje;
    }

    private void marcarFallida(Notificacion notificacion, Exception falla) {
        notificacion.setEstado("FALLIDA");
        notificacion.setUltimoError(falla.getMessage());
        notificaciones.save(notificacion);
    }
}