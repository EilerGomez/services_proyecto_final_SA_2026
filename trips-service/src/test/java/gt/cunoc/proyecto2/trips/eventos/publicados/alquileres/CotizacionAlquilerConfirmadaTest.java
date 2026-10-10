package gt.cunoc.proyecto2.trips.eventos.publicados.alquileres;

import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ClienteAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.RecorridoAlquiler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CotizacionAlquilerConfirmadaTest {

    @Test
    void desdeUnAlquilerEnLineaCotizado() {
        UUID clienteId = UUID.randomUUID();
        AlquilerPrivadoEntidad alquiler = DatosPrueba.alquiler(clienteId);
        alquiler.cotizar(new BigDecimal("2600.00"), UUID.randomUUID());

        CotizacionAlquilerConfirmada evento = CotizacionAlquilerConfirmada.desde(alquiler);

        assertThat(evento.alquilerId()).isEqualTo(alquiler.getId());
        assertThat(evento.sucursalId()).isEqualTo(XELA);
        assertThat(evento.canal()).isEqualTo(CanalAlquiler.EN_LINEA);
        assertThat(evento.clienteUsuarioId()).isEqualTo(clienteId);
        assertThat(evento.clienteNit()).isEqualTo("1234567-8");
        assertThat(evento.clienteNombre()).isEqualTo("Ana López");
        assertThat(evento.clienteCorreo()).isEqualTo("cliente@correo.com");
        assertThat(evento.origen()).isEqualTo("Quetzaltenango");
        assertThat(evento.destino()).isEqualTo("Antigua Guatemala");
        assertThat(evento.fechaSalida()).isEqualTo(OffsetDateTime.parse("2026-11-05T07:00:00-06:00"));
        assertThat(evento.fechaRetorno()).isEqualTo(OffsetDateTime.parse("2026-11-05T19:00:00-06:00"));
        assertThat(evento.cantidadPasajeros()).isEqualTo(30);
        assertThat(evento.precioConfirmado()).isEqualByComparingTo("2600.00");
    }

    @Test
    void presencialSoloIdaSinCuentaNiRetorno() {
        ClienteAlquiler cliente = new ClienteAlquiler(null, null, "Pedro Gómez", null);
        RecorridoAlquiler recorrido = new RecorridoAlquiler("Xela", "Retalhuleu",
                OffsetDateTime.parse("2026-11-10T12:00:00Z"), null, 10, new BigDecimal("60.00"));
        AlquilerPrivadoEntidad alquiler = AlquilerPrivadoEntidad.solicitar(XELA, CanalAlquiler.PRESENCIAL, cliente,
                recorrido, new BigDecimal("900.00"), DatosPrueba.FECHA, UUID.randomUUID());
        alquiler.cotizar(new BigDecimal("850.00"), UUID.randomUUID());

        CotizacionAlquilerConfirmada evento = CotizacionAlquilerConfirmada.desde(alquiler);

        assertThat(evento.canal()).isEqualTo(CanalAlquiler.PRESENCIAL);
        assertThat(evento.clienteUsuarioId()).isNull();
        assertThat(evento.clienteNit()).isEqualTo(ClienteAlquiler.CONSUMIDOR_FINAL);
        assertThat(evento.clienteCorreo()).isNull();
        assertThat(evento.fechaRetorno()).isNull();
        assertThat(evento.fechaSalida()).isEqualTo(OffsetDateTime.parse("2026-11-10T06:00:00-06:00"));
        assertThat(evento.precioConfirmado()).isEqualByComparingTo("850.00");
    }
}
