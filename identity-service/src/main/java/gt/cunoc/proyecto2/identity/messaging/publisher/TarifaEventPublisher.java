package gt.cunoc.proyecto2.identity.messaging.publisher;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.identity.evento.payload.TarifaDepreciacionPayload;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TarifaEventPublisher {

    private final PublicadorEventos publicador;

    public TarifaEventPublisher(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    public void publicarTarifaDepreciacionCambiada(
            UUID tarifaId,
            BigDecimal montoPorKm,
            LocalDate vigenteDesde,
            LocalDate fechaNegocio) {

        publicador.publicar("TarifaDepreciacion", tarifaId, "TarifaDepreciacionCambiada",
                new TarifaDepreciacionPayload(tarifaId, montoPorKm, vigenteDesde), fechaNegocio);
    }
}
