package gt.cunoc.proyecto2.fleet.dtos.sucursales;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SolicitudesSucursalTest {

    @Test
    void nuevaSucursalNormalizaYCreaLaEntidad() {
        NuevaSucursalSolicitud solicitud = new NuevaSucursalSolicitud(" xela ", " Xela Central ", "Quetzaltenango",
                "Quetzaltenango", " 4a calle ", "   ", new BigDecimal("14.8345001"), new BigDecimal("-91.518"),
                FECHA, DatosPrueba.cuenta(null, null));

        SucursalEntidad sucursal = solicitud.crearEntidad();

        assertThat(solicitud.codigo()).isEqualTo("XELA");
        assertThat(solicitud.telefono()).isNull();
        assertThat(sucursal.getId()).isNotNull();
        assertThat(sucursal.getCodigo()).isEqualTo("XELA");
        assertThat(sucursal.getNombre()).isEqualTo("Xela Central");
        assertThat(sucursal.getDireccion()).isEqualTo("4a calle");
        assertThat(sucursal.getLatitud()).isEqualByComparingTo("14.834500");
        assertThat(sucursal.getLongitud().scale()).isEqualTo(6);
        assertThat(sucursal.isActiva()).isTrue();
        assertThat(sucursal.getVersion()).isNull();
    }

    @Test
    void actualizarCopiaLosDatosEditables() {
        SucursalEntidad sucursal = DatosPrueba.sucursal(true);
        ActualizarSucursalSolicitud solicitud = new ActualizarSucursalSolicitud(" Xela Norte ", "Quetzaltenango",
                "Salcaja", "Km 200", " 7766 ", new BigDecimal("14.9"), new BigDecimal("-91.5"), 0);

        solicitud.aplicarA(sucursal);
        SucursalRespuesta respuesta = new SucursalRespuesta(sucursal);

        assertThat(respuesta.sucursalId()).isEqualTo(sucursal.getId());
        assertThat(respuesta.codigo()).isEqualTo("XELA");
        assertThat(respuesta.nombre()).isEqualTo("Xela Norte");
        assertThat(respuesta.municipio()).isEqualTo("Salcaja");
        assertThat(respuesta.direccion()).isEqualTo("Km 200");
        assertThat(respuesta.telefono()).isEqualTo("7766");
        assertThat(respuesta.latitud()).isEqualByComparingTo("14.9");
        assertThat(respuesta.activa()).isTrue();
        assertThat(respuesta.version()).isZero();
    }
}
