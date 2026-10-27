package gt.cunoc.proyecto2.sales.servicios.ventas;

import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeReasignado;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.repositorios.ventas.BoletoRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.VentaRepositorio;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A un viaje le cambiaron el bus o el piloto: hay que avisarle a quien ya tiene su boleto, porque
 * si no busca en la terminal una unidad que ya no es la suya.
 *
 * Solo se avisa a las ventas CONFIRMADAS: una cancelada o reembolsada ya no viaja, y una pendiente
 * de pago todavía no tiene asiento asegurado. Y solo a las que tienen correo: la venta de mostrador
 * puede no tenerlo, y a esas no hay cómo escribirles.
 */
@Service
public class AvisoReasignacionServicio {

    private static final Logger log = LoggerFactory.getLogger(AvisoReasignacionServicio.class);

    private final VentaRepositorio ventas;
    private final BoletoRepositorio boletos;
    private final PublicadorEventosSales eventos;

    public AvisoReasignacionServicio(VentaRepositorio ventas, BoletoRepositorio boletos,
                                     PublicadorEventosSales eventos) {
        this.ventas = ventas;
        this.boletos = boletos;
        this.eventos = eventos;
    }

    @Transactional
    public void avisarALosPasajeros(ViajeReasignado viaje) {
        List<VentaEntidad> confirmadas = ventas
                .findByViajeIdAndEstadoIn(viaje.viajeId(), List.of(EstadoVenta.CONFIRMADA)).stream()
                .filter(AvisoReasignacionServicio::tieneCorreo)
                .toList();
        if (confirmadas.isEmpty()) {
            return;
        }
        Map<UUID, List<BoletoEntidad>> porVenta = boletos
                .findByVentaIdIn(confirmadas.stream().map(VentaEntidad::getId).toList()).stream()
                .collect(Collectors.groupingBy(BoletoEntidad::getVentaId));
        for (VentaEntidad venta : confirmadas) {
            eventos.boletoReasignado(venta, porVenta.getOrDefault(venta.getId(), List.of()), viaje);
        }
        log.info("Viaje {} reasignado: se avisa a {} pasajero(s) con correo", viaje.viajeId(), confirmadas.size());
    }

    private static boolean tieneCorreo(VentaEntidad venta) {
        return venta.getClienteCorreo() != null && !venta.getClienteCorreo().isBlank();
    }
}
