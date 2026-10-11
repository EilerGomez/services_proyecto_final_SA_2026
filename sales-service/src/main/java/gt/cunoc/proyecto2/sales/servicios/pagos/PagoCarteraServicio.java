package gt.cunoc.proyecto2.sales.servicios.pagos;

import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import java.util.UUID;

/*
 * Respuestas de Identity en la saga de pago con cartera (S2 y S12).
 * Sales es el orquestador: decide qué pasa con la venta u orden y, si hace falta, pide el reembolso.
 */
public interface PagoCarteraServicio {

    void carteraDebitada(ReferenciaCartera tipo, UUID referenciaId);

    void debitoRechazado(ReferenciaCartera tipo, UUID referenciaId, String motivo);

    void carteraReembolsada(ReferenciaCartera tipo, UUID referenciaId);
}
