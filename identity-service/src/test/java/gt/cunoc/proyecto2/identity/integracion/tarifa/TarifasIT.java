package gt.cunoc.proyecto2.identity.integracion.tarifa;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** Tarifas: alta con evento, vigencia por fecha y fecha duplicada. */
class TarifasIT extends PruebaIntegracion {

    @Test
    void crearPublicaTarifaYVigenteLaEncuentra() {
        ResponseEntity<String> creada = post("/identity/tarifas-depreciacion", tokenAdmin,
                "{\"montoPorKm\":1.25,\"vigenteDesde\":\"2026-10-10\"}");
        assertThat(creada.getStatusCode().value() == 201).isTrue();

        Map<String, String> eventos = esperarEventos("TarifaDepreciacionCambiada");
        assertThat(eventos.get("TarifaDepreciacionCambiada")).contains("1.25");

        ResponseEntity<String> vigente =
                get("/identity/tarifas-depreciacion?vigenteEn=2026-10-20", tokenAdmin);
        assertThat(vigente.getStatusCode().value() == 200).isTrue();
        assertThat(PruebaIntegracion.<Double>json(vigente, "$.montoPorKm")).isEqualTo(1.25);
    }

    @Test
    void fechaRepetidaDaConflicto() {
        post("/identity/tarifas-depreciacion", tokenAdmin,
                "{\"montoPorKm\":1.25,\"vigenteDesde\":\"2026-10-10\"}");

        esError(post("/identity/tarifas-depreciacion", tokenAdmin,
                "{\"montoPorKm\":2.00,\"vigenteDesde\":\"2026-10-10\"}"),
                409, "TARIFA_YA_EXISTE_PARA_FECHA");
    }
}
