package gt.cunoc.proyecto2.notification.services.correo;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.Barcode39;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoVendido;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private static final Color ACENTO = new Color(37, 99, 235);
    private static final Color GRIS = new Color(100, 116, 139);
    private static final Color TEXTO = new Color(30, 41, 59);

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.WHITE);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.WHITE);
    private static final Font ETIQUETA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, GRIS);
    private static final Font DATO = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXTO);
    private static final Font DATO_FUERTE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, ACENTO);
    private static final Font NOTA = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
    private static final Font CODIGO = FontFactory.getFont(FontFactory.COURIER, 9, TEXTO);

    /** PDF listo para adjuntar; nunca devuelve un arreglo vacío. */
    public byte[] generar(BoletoVendido compra) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A5.rotate(), 28, 28, 24, 24);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(cabecera(compra));
            documento.add(new Paragraph(" ", ETIQUETA));
            documento.add(datos(compra));
            documento.add(codigoBarras(compra));
            documento.add(centrado(compra.ventaId().toString(), CODIGO));
            documento.add(centrado(
                    "Presenta este código al abordar. Llega 30 minutos antes de la salida.", NOTA));
            documento.close();
        } catch (DocumentException | IOException falla) {
            throw new IllegalStateException("No se pudo generar el PDF del boleto " + compra.ventaId(), falla);
        }
        return salida.toByteArray();
    }

    private static PdfPTable cabecera(BoletoVendido compra) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBorder(0);
        celda.setBackgroundColor(AZUL);
        celda.setPadding(14);
        Paragraph titulo = new Paragraph("BOLETO DE VIAJE", TITULO);
        titulo.setSpacingAfter(4);
        celda.addElement(titulo);
        celda.addElement(new Paragraph(
                valor(compra.origenNombre()) + "  →  " + valor(compra.destinoNombre()), SUBTITULO));
        tabla.addCell(celda);
        return tabla;
    }

    private static PdfPTable datos(BoletoVendido compra) {
        PdfPTable tabla = new PdfPTable(3);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] { 1.2f, 1f, 1f });

        tabla.addCell(etiqueta("SALIDA"));
        tabla.addCell(etiqueta("BUS"));
        tabla.addCell(etiqueta("TOTAL"));
        tabla.addCell(dato(CorreoVentasService.fechaHora(compra.fechaSalida()), DATO));
        tabla.addCell(dato(valor(compra.busPlaca()), DATO));
        tabla.addCell(dato(CorreoVentasService.quetzales(compra.montoTotal()), DATO_FUERTE));

        tabla.addCell(etiqueta("PASAJERO"));
        tabla.addCell(etiqueta("NIT"));
        tabla.addCell(etiqueta("ASIENTOS"));
        tabla.addCell(dato(valor(compra.clienteNombre()), DATO));
        tabla.addCell(dato(valor(compra.clienteNit()), DATO));
        tabla.addCell(dato(CorreoVentasService.asientos(compra.asientos()), DATO));

        PdfPCell pie = new PdfPCell(new Phrase("Pagado el " + CorreoVentasService.fecha(compra.fechaPago())
                + " con " + CorreoVentasService.metodo(compra.metodoPago()) + ".", NOTA));
        pie.setColspan(3);
        pie.setBorder(0);
        pie.setPaddingTop(10);
        tabla.addCell(pie);
        return tabla;
    }

    private static Image codigoBarras(BoletoVendido compra) throws DocumentException, IOException {
        Barcode39 codigo = new Barcode39();
        codigo.setCode(compra.ventaId().toString().toUpperCase(Locale.ROOT));
        codigo.setStartStopText(false);
        codigo.setBarHeight(26f);
        codigo.setX(1.1f);
        Image imagen = Image.getInstance(codigo.createAwtImage(Color.BLACK, Color.WHITE), null);
        imagen.scaleToFit(220, 42);
        imagen.setAlignment(Element.ALIGN_CENTER);
        return imagen;
    }

    private static Paragraph centrado(String texto, Font fuente) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_CENTER);
        return parrafo;
    }

    private static PdfPCell etiqueta(String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, ETIQUETA));
        celda.setBorder(0);
        celda.setPaddingTop(8);
        return celda;
    }

    private static PdfPCell dato(String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBorder(0);
        celda.setPaddingBottom(4);
        return celda;
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }
}
