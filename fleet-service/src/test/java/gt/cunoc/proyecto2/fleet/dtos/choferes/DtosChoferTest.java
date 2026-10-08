package gt.cunoc.proyecto2.fleet.dtos.choferes;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import gt.cunoc.proyecto2.fleet.modelos.choferes.TipoLicencia;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtosChoferTest {

    private final UUID sucursalId = UUID.randomUUID();

    @Test
    void nuevoChoferNormalizaLaLicenciaYCreaLaEntidad() {
        NuevoChoferSolicitud solicitud = new NuevoChoferSolicitud(sucursalId, " lic-1234 ", TipoLicencia.A,
                FECHA.plusYears(1), new BigDecimal("300.00"), FECHA, DatosPrueba.cuenta(null, DatosPrueba.FOTO_CHOFER));
        UUID choferId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        ChoferEntidad chofer = solicitud.crearEntidad(choferId, usuarioId, "Ana Lopez");
        ChoferRespuesta respuesta = new ChoferRespuesta(chofer);

        assertThat(solicitud.numeroLicencia()).isEqualTo("LIC-1234");
        assertThat(respuesta.choferId()).isEqualTo(choferId);
        assertThat(respuesta.usuarioId()).isEqualTo(usuarioId);
        assertThat(respuesta.sucursalId()).isEqualTo(sucursalId);
        assertThat(respuesta.nombreCompleto()).isEqualTo("Ana Lopez");
        assertThat(respuesta.numeroLicencia()).isEqualTo("LIC-1234");
        assertThat(respuesta.tipoLicencia()).isEqualTo(TipoLicencia.A);
        assertThat(respuesta.salarioBaseViaje()).isEqualByComparingTo("300.00");
        assertThat(respuesta.estado()).isEqualTo(EstadoChofer.ACTIVO);
        assertThat(respuesta.version()).isNull();
    }

    @Test
    void actualizarLicenciaNormalizaYAplica() {
        ChoferEntidad chofer = DatosPrueba.chofer(sucursalId);
        ActualizarLicenciaSolicitud solicitud = new ActualizarLicenciaSolicitud(" nueva-77 ", TipoLicencia.B,
                FECHA.plusYears(4), FECHA, 0);

        solicitud.aplicarA(chofer);

        assertThat(chofer.getNumeroLicencia()).isEqualTo("NUEVA-77");
        assertThat(chofer.getTipoLicencia()).isEqualTo(TipoLicencia.B);
        assertThat(chofer.getFechaVencimientoLicencia()).isEqualTo(FECHA.plusYears(4));
    }

    @Test
    void respuestaInternaCalculaLaVigenciaConLaFechaDelViaje() {
        ChoferEntidad chofer = DatosPrueba.chofer(sucursalId);
        var vencimiento = chofer.getFechaVencimientoLicencia();

        ChoferInternoRespuesta vigente = new ChoferInternoRespuesta(chofer, vencimiento);
        ChoferInternoRespuesta vencida = new ChoferInternoRespuesta(chofer, vencimiento.plusDays(1));

        assertThat(vigente.licenciaVigente()).isTrue();
        assertThat(vigente.choferId()).isEqualTo(chofer.getId());
        assertThat(vigente.salarioBaseViaje()).isEqualByComparingTo("350.00");
        assertThat(vigente.estado()).isEqualTo(EstadoChofer.ACTIVO);
        assertThat(vencida.licenciaVigente()).isFalse();
    }
}
