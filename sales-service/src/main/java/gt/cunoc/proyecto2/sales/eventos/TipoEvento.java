package gt.cunoc.proyecto2.sales.eventos;

/**
 * Nombres de los eventos (son la routing key en RabbitMQ). Deben coincidir con el catálogo
 * de CLAUDE.md y con comun.eventos.suscripciones del application.yml.
 */
public enum TipoEvento {

    // ----- Publica Sales (exchange sales.eventos) -----
    DEBITO_CARTERA_SOLICITADO("DebitoCarteraSolicitado"),
    REEMBOLSO_CARTERA_SOLICITADO("ReembolsoCarteraSolicitado"),
    BOLETO_VENDIDO("BoletoVendido"),
    VENTA_CANCELADA("VentaCancelada"),
    VENTA_REEMBOLSADA("VentaReembolsada"),
    ALQUILER_PAGADO("AlquilerPagado"),

    // ----- Consume Sales (cola sales.q) -----
    VIAJE_PROGRAMADO("ViajeProgramado"),
    VIAJE_ACTUALIZADO("ViajeActualizado"),
    VIAJE_INICIADO("ViajeIniciado"),
    VIAJE_ELIMINADO("ViajeEliminado"),
    COTIZACION_ALQUILER_CONFIRMADA("CotizacionAlquilerConfirmada"),
    CLIENTE_REGISTRADO("ClienteRegistrado"),
    PERFIL_ACTUALIZADO("PerfilActualizado"),
    CARTERA_DEBITADA("CarteraDebitada"),
    DEBITO_CARTERA_RECHAZADO("DebitoCarteraRechazado"),
    CARTERA_REEMBOLSADA("CarteraReembolsada");

    private final String nombre;

    TipoEvento(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
