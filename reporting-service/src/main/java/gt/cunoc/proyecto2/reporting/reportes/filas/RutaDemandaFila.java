package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** RG4: ruta demandada por boletos (las cancelaciones restan). */
@Value
@Builder
public class RutaDemandaFila {

    UUID rutaId;
    String origen;
    String destino;
    Long boletosVendidos;
    Long boletosCancelados;
    Long boletos;
    BigDecimal ingresos;
}
