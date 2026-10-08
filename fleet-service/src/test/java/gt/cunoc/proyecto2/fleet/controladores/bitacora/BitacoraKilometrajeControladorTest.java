package gt.cunoc.proyecto2.fleet.controladores.bitacora;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.dtos.bitacora.ViajeKilometrajeRespuesta;
import gt.cunoc.proyecto2.fleet.modelos.bitacora.BitacoraKilometrajeEntidad;
import gt.cunoc.proyecto2.fleet.servicios.bitacora.BitacoraKilometrajeServicio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BitacoraKilometrajeControladorTest {

    private final BitacoraKilometrajeServicio servicio = mock(BitacoraKilometrajeServicio.class);
    private final BitacoraKilometrajeControlador controlador = new BitacoraKilometrajeControlador(servicio);

    @Test
    void listarConvierteLosRegistros() {
        UUID busId = UUID.randomUUID();
        UsuarioActual admin = DatosPrueba.adminSistema();
        OffsetDateTime llegada = OffsetDateTime.parse("2026-10-20T17:00:00-06:00");
        BitacoraKilometrajeEntidad registro = BitacoraKilometrajeEntidad.registrar(busId, UUID.randomUUID(),
                new BigDecimal("100.00"), new BigDecimal("350.00"), llegada);
        when(servicio.listar(busId, FECHA.minusDays(7), FECHA, admin)).thenReturn(List.of(registro));

        ResponseEntity<List<ViajeKilometrajeRespuesta>> respuesta =
                controlador.listar(busId, FECHA.minusDays(7), FECHA, admin);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).singleElement().satisfies(v -> {
            assertThat(v.viajeId()).isEqualTo(registro.getViajeId());
            assertThat(v.kmInicial()).isEqualByComparingTo("100.00");
            assertThat(v.kmFinal()).isEqualByComparingTo("350.00");
            assertThat(v.kmRecorridos()).isEqualByComparingTo("250.00");
            assertThat(v.fechaLlegada()).isEqualTo(llegada);
        });
    }
}
