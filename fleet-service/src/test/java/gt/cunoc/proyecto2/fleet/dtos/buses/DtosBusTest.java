package gt.cunoc.proyecto2.fleet.dtos.buses;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtosBusTest {

    private final UUID sucursalId = UUID.randomUUID();

    @Test
    void nuevoBusNormalizaYCreaLaEntidad() {
        NuevoBusSolicitud solicitud = new NuevoBusSolicitud(sucursalId, " p-123abc ", " Volvo ", " B12 ",
                2018, 50, null, " " + DatosPrueba.FOTO_BUS + " ", FECHA);

        BusEntidad bus = solicitud.crearEntidad();

        assertThat(solicitud.placa()).isEqualTo("P-123ABC");
        assertThat(solicitud.kilometrajeActual()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bus.getSucursalId()).isEqualTo(sucursalId);
        assertThat(bus.getMarca()).isEqualTo("Volvo");
        assertThat(bus.getModelo()).isEqualTo("B12");
        assertThat(bus.getAnioFabricacion()).isEqualTo(2018);
        assertThat(bus.getCapacidadPasajeros()).isEqualTo(50);
        assertThat(bus.getFotoUrl()).isEqualTo(DatosPrueba.FOTO_BUS);
        assertThat(bus.getEstado()).isEqualTo(EstadoBus.ACTIVO);
    }

    @Test
    void actualizarCopiaLosDatosEditables() {
        BusEntidad bus = DatosPrueba.bus(sucursalId);
        String foto = DatosPrueba.URL_FOTOS + "dev/buses/nueva.png";

        new ActualizarBusSolicitud(" Mercedes ", " O500 ", 2020, 45, foto, 0).aplicarA(bus);
        BusRespuesta respuesta = new BusRespuesta(bus);

        assertThat(respuesta.busId()).isEqualTo(bus.getId());
        assertThat(respuesta.placa()).isEqualTo("P-123ABC");
        assertThat(respuesta.marca()).isEqualTo("Mercedes");
        assertThat(respuesta.modelo()).isEqualTo("O500");
        assertThat(respuesta.anioFabricacion()).isEqualTo(2020);
        assertThat(respuesta.capacidadPasajeros()).isEqualTo(45);
        assertThat(respuesta.fotoUrl()).isEqualTo(foto);
        assertThat(respuesta.version()).isZero();
    }

    @Test
    void disponibilidadSegunElEstado() {
        BusEntidad activo = DatosPrueba.bus(sucursalId);
        BusEntidad enRuta = DatosPrueba.bus(sucursalId);
        enRuta.registrarSalida();
        BusEntidad enTaller = DatosPrueba.bus(sucursalId);
        enTaller.cambiarEstado(EstadoBus.EN_TALLER);
        BusEntidad inactivo = DatosPrueba.bus(sucursalId);
        inactivo.cambiarEstado(EstadoBus.INACTIVO);

        DisponibilidadBusRespuesta disponible = new DisponibilidadBusRespuesta(activo);
        assertThat(disponible.disponible()).isTrue();
        assertThat(disponible.motivo()).isNull();
        assertThat(disponible.capacidadPasajeros()).isEqualTo(50);
        assertThat(new DisponibilidadBusRespuesta(enRuta).disponible()).isTrue();
        assertThat(new DisponibilidadBusRespuesta(enTaller).motivo()).isEqualTo("BUS_EN_TALLER");
        assertThat(new DisponibilidadBusRespuesta(inactivo).disponible()).isFalse();
        assertThat(new DisponibilidadBusRespuesta(inactivo).motivo()).isEqualTo("BUS_INACTIVO");
    }
}
