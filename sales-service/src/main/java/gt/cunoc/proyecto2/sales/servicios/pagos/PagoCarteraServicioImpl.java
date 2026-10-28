package gt.cunoc.proyecto2.sales.servicios.pagos;

import gt.cunoc.proyecto2.sales.eventos.MotivoSaga;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.OrdenAlquilerRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.asientos.AsientoViajeRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.BoletoRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.VentaRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Saga de pago con cartera. Corre dentro de la transacción del consumidor (idempotente por eventId):
 * si algo falla, se revierte todo (también los eventos del outbox) y el mensaje se reintenta.
 */
@Service
public class PagoCarteraServicioImpl implements PagoCarteraServicio {

    private static final Logger log = LoggerFactory.getLogger(PagoCarteraServicioImpl.class);
    private static final String MOTIVO_DESCONOCIDO = "RECHAZADO";

    private final VentaRepositorio ventas;
    private final BoletoRepositorio boletos;
    private final AsientoViajeRepositorio asientos;
    private final VistaViajeRepositorio viajes;
    private final OrdenAlquilerRepositorio ordenes;
    private final PublicadorEventosSales eventos;

    public PagoCarteraServicioImpl(VentaRepositorio ventas, BoletoRepositorio boletos,
                                   AsientoViajeRepositorio asientos, VistaViajeRepositorio viajes,
                                   OrdenAlquilerRepositorio ordenes, PublicadorEventosSales eventos) {
        this.ventas = ventas;
        this.boletos = boletos;
        this.asientos = asientos;
        this.viajes = viajes;
        this.ordenes = ordenes;
        this.eventos = eventos;
    }

    //CarteraDebitada

    @Override
    @Transactional
    public void carteraDebitada(ReferenciaCartera tipo, UUID referenciaId) {
        switch (tipo) {
            case VENTA -> ventaDebitada(referenciaId);
            case ALQUILER -> ordenDebitada(referenciaId);
        }
    }

    private void ventaDebitada(UUID ventaId) {
        VentaEntidad venta = ventas.buscarParaActualizar(ventaId).orElse(null);
        if (venta == null) {
            log.warn("CarteraDebitada de la venta {} que no existe: se ignora", ventaId);
            return;
        }
        switch (venta.getEstado()) {
            case PENDIENTE_PAGO -> confirmarVenta(venta);
            // Venció la reserva o se eliminó el viaje antes de que Wallet cobrara: se devuelve
            case RECHAZADA -> eventos.reembolsoVenta(venta, MotivoSaga.DEBITO_TARDIO,
                    venta.fechaNoAnteriorAlPago(Fechas.hoy()));
            // CONFIRMADA: respuesta repetida. CANCELADA / REEMBOLSADA: el reembolso ya se pidió al cancelar
            default -> log.info("CarteraDebitada repetida para la venta {} ({}): no se hace nada",
                    ventaId, venta.getEstado());
        }
    }

    private void confirmarVenta(VentaEntidad venta) {
        VistaViajeEntidad viaje = viajes.findById(venta.getViajeId())
                .filter(VistaViajeEntidad::estaALaVenta)
                .orElse(null);
        List<Integer> numeros = asientos.reservadosDeVenta(venta.getId());
        if (viaje == null || numeros.size() != venta.getCantidadBoletos()) {
            MotivoSaga motivo = viaje == null ? MotivoSaga.VIAJE_NO_DISPONIBLE : MotivoSaga.TIEMPO_AGOTADO;
            venta.rechazar(motivo.name());
            VentaEntidad rechazada = ventas.saveAndFlush(venta);
            asientos.liberarDeVenta(rechazada.getId());
            eventos.reembolsoVenta(rechazada, MotivoSaga.DEBITO_TARDIO,
                    rechazada.fechaNoAnteriorAlPago(Fechas.hoy()));
            log.info("Venta {} rechazada al llegar el débito ({}): se pide el reembolso", venta.getId(), motivo);
            return;
        }
        asientos.confirmarReservas(venta.getId());
        venta.confirmarPago();
        VentaEntidad confirmada = ventas.saveAndFlush(venta);
        List<BoletoEntidad> emitidos = boletos.saveAll(numeros.stream()
                .map(numero -> BoletoEntidad.emitir(confirmada, numero, confirmada.precioPorBoleto()))
                .toList());
        eventos.boletoVendido(confirmada, emitidos, viaje);
    }

    private void ordenDebitada(UUID ordenId) {
        OrdenAlquilerEntidad orden = ordenes.findById(ordenId).orElse(null);
        if (orden == null) {
            log.warn("CarteraDebitada de la orden {} que no existe: se ignora", ordenId);
            return;
        }
        if (orden.cobroConCarteraEnCurso()) {
            orden.confirmarPagoConCartera();
            eventos.alquilerPagado(ordenes.saveAndFlush(orden));
        } else if (orden.pagadaConCartera()) {
            log.info("CarteraDebitada repetida para la orden {}: no se hace nada", ordenId);
        } else {
            // Se pagó en efectivo mientras Wallet cobraba (u otro estado que ya no espera el cobro)
            eventos.reembolsoOrden(orden, MotivoSaga.DEBITO_TARDIO, orden.fechaNoAnteriorAlPago(Fechas.hoy()));
            log.info("Débito tardío de la orden {} ({}): se pide el reembolso", ordenId, orden.getEstado());
        }
    }

    // DebitoCarteraRechazado 

    @Override
    @Transactional
    public void debitoRechazado(ReferenciaCartera tipo, UUID referenciaId, String motivo) {
        String razon = motivo == null || motivo.isBlank() ? MOTIVO_DESCONOCIDO : motivo;
        switch (tipo) {
            case VENTA -> ventaRechazada(referenciaId, razon);
            case ALQUILER -> ordenRechazada(referenciaId, razon);
        }
    }

    private void ventaRechazada(UUID ventaId, String motivo) {
        VentaEntidad venta = ventas.buscarParaActualizar(ventaId).orElse(null);
        if (venta == null || !venta.estaPendienteDePago()) {
            log.info("DebitoCarteraRechazado para la venta {} que ya no está pendiente: no se hace nada", ventaId);
            return;
        }
        venta.rechazar(motivo);
        ventas.saveAndFlush(venta);
        asientos.liberarDeVenta(ventaId);
    }

    private void ordenRechazada(UUID ordenId, String motivo) {
        OrdenAlquilerEntidad orden = ordenes.findById(ordenId).orElse(null);
        if (orden == null || !orden.cobroConCarteraEnCurso()) {
            log.info("DebitoCarteraRechazado para la orden {} sin cobro en curso: no se hace nada", ordenId);
            return;
        }
        orden.rechazarPagoConCartera(motivo);
        ordenes.saveAndFlush(orden);
    }

    // CarteraReembolsada 

    @Override
    @Transactional
    public void carteraReembolsada(ReferenciaCartera tipo, UUID referenciaId) {
        if (tipo == ReferenciaCartera.ALQUILER) {
            ordenReembolsada(referenciaId);
            return;
        }
        VentaEntidad venta = ventas.findById(referenciaId).orElse(null);
        if (venta == null || !venta.puedeMarcarseReembolsada()) {
            log.info("CarteraReembolsada para la venta {} ({}): no se hace nada", referenciaId,
                    venta == null ? "no existe" : venta.getEstado());
            return;
        }
        venta.marcarReembolsada(Fechas.hoy());
        ventas.saveAndFlush(venta);
    }

    /**
     * El dinero del alquiler ya volvió a la cartera. Solo cambia de estado la orden que estaba
     * esperando ese reembolso (CANCELADA): el reembolso de un cobro de más (débito tardío) no la toca,
     * porque esa orden debe poder seguir pagándose o quedar PAGADA en efectivo.
     */
    private void ordenReembolsada(UUID ordenId) {
        OrdenAlquilerEntidad orden = ordenes.findById(ordenId).orElse(null);
        if (orden == null || !orden.puedeMarcarseReembolsada()) {
            log.info("CarteraReembolsada para la orden {} ({}): no se hace nada", ordenId,
                    orden == null ? "no existe" : orden.getEstado());
            return;
        }
        orden.marcarReembolsada(Fechas.hoy(), null);         // la cartera no la entrega ningún cajero
        ordenes.saveAndFlush(orden);
        log.info("Orden {} reembolsada por la cartera", ordenId);
    }
}
