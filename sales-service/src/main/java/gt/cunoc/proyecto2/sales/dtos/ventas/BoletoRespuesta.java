package gt.cunoc.proyecto2.sales.dtos.ventas;

import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoBoleto;
import java.math.BigDecimal;
import java.util.UUID;

public record BoletoRespuesta(UUID boletoId, int numeroAsiento, BigDecimal precio, EstadoBoleto estado) {

    public BoletoRespuesta(BoletoEntidad boleto) {
        this(boleto.getId(), boleto.getNumeroAsiento(), boleto.getPrecio(), boleto.getEstado());
    }
}