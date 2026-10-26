package gt.cunoc.proyecto2.notification.services.correo;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoVendido;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Arma el PDF del boleto que se adjunta al correo de una compra (en línea o en mostrador).
 *
 * <p>Mismo contenido que el boleto de la aplicación: ruta, salida, bus, asientos, pasajero, NIT y
 * total, más el número de venta en código de barras. Tamaño A5 horizontal para que se imprima en
 * media hoja.
 */
@Service
public class BoletoPdfService {

    private static final Color AZUL = new Color(15, 23, 42);
    private static final Color GRIS = new Color(203, 213, 225);
    private static final Color TEXTO = new Color(30, 41, 59);
    private static final Color ACENTO = new Color(37, 99, 235);
    private static final Color BLANCO = Color.WHITE;
    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Guatemala");
    private static final DateTimeFormatter MES_ANIO =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-GT")).withZone(ZONA_LOCAL);

    private static final Font FECHA = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
    private static final Font RUTA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 19, BLANCO);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS);
    private static final Font ETIQUETA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(100, 116, 139));
    private static final Font DATO = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXTO);
    private static final Font DATO_FUERTE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, ACENTO);
    private static final Font NOTA = FontFactory.getFont(FontFactory.HELVETICA, 7, GRIS);
    private static final Font CODIGO = FontFactory.getFont(FontFactory.COURIER, 8, GRIS);

    /** PDF listo para adjuntar; nunca devuelve un arreglo vacío. */
    public byte[] generar(BoletoVendido compra) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A5.rotate(), 20, 20, 20, 20);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(boleto(compra));
            documento.close();
        } catch (DocumentException | IOException falla) {
            throw new IllegalStateException("No se pudo generar el PDF del boleto " + compra.ventaId(), falla);
        }
        return salida.toByteArray();
    }

    private static PdfPTable boleto(BoletoVendido compra) throws DocumentException, IOException {
        PdfPTable boleto = new PdfPTable(new float[] { 4.4f, 1.3f });
        boleto.setWidthPercentage(100);
        boleto.setKeepTogether(true);
        boleto.addCell(contenidoPrincipal(compra));
        boleto.addCell(talon(compra));
        return boleto;
    }

    private static PdfPCell contenidoPrincipal(BoletoVendido compra) throws DocumentException {
        PdfPCell principal = new PdfPCell();
        principal.setBorder(0);
        principal.setBackgroundColor(AZUL);
        principal.setPadding(16);
        principal.addElement(new Paragraph("SALIDA · " + mesAnio(compra.fechaSalida()), FECHA));
        Paragraph ruta = new Paragraph(
                valor(compra.origenNombre()) + " → " + valor(compra.destinoNombre()), RUTA);
        ruta.setSpacingBefore(4);
        ruta.setSpacingAfter(2);
        principal.addElement(ruta);
        principal.addElement(new Paragraph(
                CorreoVentasService.fechaHora(compra.fechaSalida()) + " · Bus " + valor(compra.busPlaca()),
                SUBTITULO));

        PdfPTable datos = new PdfPTable(2);
        datos.setWidthPercentage(100);
        datos.setSpacingBefore(12);
        datos.setSpacingAfter(8);
        datos.addCell(caja("ASIENTOS", CorreoVentasService.asientos(compra.asientos()), false));
        datos.addCell(caja("TOTAL", CorreoVentasService.quetzales(compra.montoTotal()), true));
        datos.addCell(caja("PASAJERO", valor(compra.clienteNombre()), false));
        datos.addCell(caja("ESTADO", "Confirmada", false));
        principal.addElement(datos);
        principal.addElement(new Paragraph("NIT " + valor(compra.clienteNit())
                + " · Presenta este código al abordar. Llega 30 minutos antes.", NOTA));
        Paragraph notaDevolucion = new Paragraph(
                "Guarda el boleto: con su código se hace cualquier devolución en la sucursal.", NOTA);
        notaDevolucion.setSpacingBefore(5);
        principal.addElement(notaDevolucion);
        return principal;
    }

    private static PdfPCell caja(String titulo, String texto, boolean destacado) {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(0);
        celda.setBackgroundColor(BLANCO);
        celda.setPadding(8);
        celda.addElement(new Paragraph(titulo, ETIQUETA));
        celda.addElement(new Paragraph(texto, destacado ? DATO_FUERTE : DATO));
        return celda;
    }

    private static PdfPCell talon(BoletoVendido compra) throws DocumentException, IOException {
        PdfPCell talon = new PdfPCell();
        talon.setBorder(0);
        talon.setBackgroundColor(AZUL);
        talon.setBorderWidthLeft(1);
        talon.setBorderColorLeft(new Color(148, 163, 184));
        talon.setPadding(10);
        Image imagen = codigoBarras(compra);
        imagen.scaleToFit(72, 190);
        imagen.setAlignment(Element.ALIGN_CENTER);
        talon.addElement(imagen);
        Paragraph codigo = centrado(codigoLegible(codigo(compra)), CODIGO);
        codigo.setSpacingBefore(6);
        talon.addElement(codigo);
        talon.addElement(centrado("C Ó D I G O", FECHA));
        return talon;
    }

    private static Image codigoBarras(BoletoVendido compra) throws DocumentException, IOException {
        Barcode128 codigo = new Barcode128();
        codigo.setCode(codigo(compra));
        codigo.setCodeType(Barcode128.CODE128);
        codigo.setBarHeight(74f);
        codigo.setX(1.5f);
        java.awt.Image codigoOriginal = codigo.createAwtImage(Color.BLACK, Color.WHITE);
        BufferedImage horizontal = new BufferedImage(codigoOriginal.getWidth(null), codigoOriginal.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D horizontalGraphics = horizontal.createGraphics();
        try {
            horizontalGraphics.setColor(Color.WHITE);
            horizontalGraphics.fillRect(0, 0, horizontal.getWidth(), horizontal.getHeight());
            horizontalGraphics.drawImage(codigoOriginal, 0, 0, null);
        } finally {
            horizontalGraphics.dispose();
        }
        BufferedImage vertical = new BufferedImage(horizontal.getHeight(), horizontal.getWidth(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = vertical.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, vertical.getWidth(), vertical.getHeight());
            graphics.translate(0, horizontal.getWidth());
            graphics.rotate(-Math.PI / 2);
            graphics.drawImage(horizontal, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return Image.getInstance(vertical, null);
    }

    private static String codigo(BoletoVendido compra) {
        return compra.ventaId().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private static String codigoLegible(String codigo) {
        return codigo.replaceAll("(.{4})(?=.)", "$1-");
    }

    private static String mesAnio(OffsetDateTime fecha) {
        return fecha == null ? "-" : MES_ANIO.format(fecha);
    }

    private static Paragraph centrado(String texto, Font fuente) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_CENTER);
        return parrafo;
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }
}
