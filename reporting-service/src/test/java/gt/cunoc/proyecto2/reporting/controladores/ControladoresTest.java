package gt.cunoc.proyecto2.reporting.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.FabricaGeneradores;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControladoresTest {

    @Mock
    FabricaGeneradores fabrica;
    @Mock
    SucursalRepositorio sucursales;
    @Mock
    GeneradorReporte generador;

    private ReporteSistemaControlador sistema;
    private ReporteSucursalControlador sucursal;

    private final UUID id = UUID.randomUUID();

    private UsuarioActual adminSistema() {
        return new UsuarioActual(UUID.randomUUID(), "a@r.gt", Set.of(Roles.ADMIN_SISTEMA), null, null);
    }

    private UsuarioActual adminSucursal() {
        return new UsuarioActual(UUID.randomUUID(), "s@r.gt", Set.of(Roles.ADMIN_SUCURSAL), id, null);
    }

    @BeforeEach
    void armar() throws Exception {
        when(fabrica.obtener(any())).thenReturn(generador);
        when(generador.datos(any())).thenReturn(Map.of("ok", true));
        when(generador.generar(any())).thenReturn("%PDF-falso".getBytes());
        when(sucursales.existsById(id)).thenReturn(true);
        sistema = new ReporteSistemaControlador(fabrica);
        sucursal = new ReporteSucursalControlador(fabrica, sucursales);
    }

    @Test
    void sistemaExponeLosSeisJsonYSusPdf() throws Exception {
        assertThat(sistema.ingresos(null, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sistema.gastos(null, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sistema.ganancias(null, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sistema.rutasDemandadas(null, null, 5).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sistema.tallerPorCategoria(null, null, null, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(sistema.mapaRutas(null).getStatusCode()).isEqualTo(HttpStatus.OK);

        var pdf = sistema.gananciasPdf(null, null, null);
        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(pdf.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("ganancias_todos_todos.pdf");
        assertThat(sistema.ingresosPdf(null, null, null).getHeaders()
                .getContentDisposition().getFilename()).startsWith("ingresos_");
        assertThat(sistema.gastosPdf(null, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sistema.rutasDemandadasPdf(null, null, 5).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sistema.tallerPorCategoriaPdf(null, null, null, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(sistema.mapaRutasPdf(null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void sucursalExponeLosSeisJsonYSusPdf() throws Exception {
        var usuario = adminSucursal();
        assertThat(sucursal.buses(id, null, usuario).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sucursal.choferes(id, null, usuario).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sucursal.ingresosBoletos(id, null, null, null, null, usuario).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(sucursal.ingresosAlquiler(id, null, null, usuario).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(sucursal.depreciacion(id, null, null, null, usuario).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(sucursal.tallerPorBus(id, null, null, null, usuario).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(sucursal.busesPdf(id, null, usuario).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sucursal.choferesPdf(id, null, usuario).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sucursal.ingresosBoletosPdf(id, null, null, null, null, usuario).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(sucursal.ingresosAlquilerPdf(id, null, null, usuario).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(sucursal.depreciacionPdf(id, null, null, null, usuario).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(sucursal.tallerPorBusPdf(id, null, null, null, usuario).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        // el admin del sistema también entra
        assertThat(sucursal.buses(id, null, adminSistema()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void sucursalAjenaEs403() {
        var otra = new UsuarioActual(UUID.randomUUID(), "o@r.gt",
                Set.of(Roles.ADMIN_SUCURSAL), UUID.randomUUID(), null);
        var lanzada = org.assertj.core.api.Assertions.catchThrowable(
                () -> sucursal.buses(id, null, otra));
        assertThat(lanzada).isInstanceOf(ProhibidoException.class);
        assertThat(((ProhibidoException) lanzada).getCodigo()).isEqualTo("SUCURSAL_AJENA");
    }

    @Test
    void sucursalInexistenteEs404() {
        UUID falta = UUID.randomUUID();
        when(sucursales.existsById(falta)).thenReturn(false);
        assertThatThrownBy(() -> sucursal.buses(falta, null, adminSistema()))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void fabricaSinGeneradorEs404() {
        when(fabrica.obtener(TipoReporte.GANANCIAS)).thenThrow(
                new NoEncontradoException("REPORTE_NO_EXISTE", "No existe"));
        assertThatThrownBy(() -> sistema.ganancias(null, null, null))
                .isInstanceOf(NoEncontradoException.class);
    }
}
