package gt.cunoc.proyecto2.notification.services.correo;

/**
 * Archivo que viaja pegado a un correo (por ahora, el PDF del boleto de una compra).
 *
 * @param nombre         nombre con el que el destinatario ve el archivo
 * @param contenido      bytes del archivo
 * @param tipoContenido  MIME del archivo, por ejemplo {@code application/pdf}
 */
public record AdjuntoCorreo(String nombre, byte[] contenido, String tipoContenido) {
}
