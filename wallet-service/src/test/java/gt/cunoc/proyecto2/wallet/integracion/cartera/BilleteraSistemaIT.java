package gt.cunoc.proyecto2.wallet.integracion.cartera;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.wallet.integracion.soporte.PruebaIntegracion;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * W5 · filtros de los movimientos de la billetera del sistema: sin tipo salen los dos (cobros y
 * devoluciones), con un tipo que no existe se rechaza, y el rango de fechas se respeta.
 */
class BilleteraSistemaIT extends PruebaIntegracion {

    private String tokenAdminSistema() {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "admin@pruebas.gt", Set.of(Roles.ADMIN_SISTEMA),
                null, null));
    }

    private void solicitarDebito(UUID usuarioId, UUID referencia, String monto) {
        publicarEvento(SALES_EVENTOS, "DebitoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":%s,"fechaMovimiento":"%s"}
                """.formatted(referencia, usuarioId, monto, FECHA));
    }

    private void solicitarReembolso(UUID usuarioId, UUID referencia, String monto) {
        publicarEvento(SALES_EVENTOS, "ReembolsoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":%s,"motivo":"CANCELACION","fechaMovimiento":"%s"}
                """.formatted(referencia, usuarioId, monto, FECHA));
    }

    /** Una compra pagada con cartera y luego reembolsada: deja un COBRO y una DEVOLUCION. */
    private void compraYReembolso() {
        UUID usuarioId = UUID.randomUUID();
        UUID compra = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        solicitarDebito(usuarioId, compra, "150.00");
        esperarEventos("CarteraDebitada");
        solicitarReembolso(usuarioId, compra, "150.00");
        esperarEventos("CarteraReembolsada");
    }

    @Test
    void sinFiltroDeTipoSalenElCobroYLaDevolucionDelRango() {
        compraYReembolso();

        ResponseEntity<String> movimientos = get(
                "/wallet/sistema/movimientos?desde=2026-10-01&hasta=2026-10-31", tokenAdminSistema());

        assertThat(movimientos.getStatusCode().value()).as(movimientos.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<List<String>>json(movimientos, "$[*].tipo"))
                .containsExactlyInAnyOrder("COBRO", "DEVOLUCION");

        // Y el rango se respeta: los dos movimientos son de octubre
        ResponseEntity<String> noviembre = get(
                "/wallet/sistema/movimientos?desde=2026-11-01&hasta=2026-11-30", tokenAdminSistema());
        assertThat(PruebaIntegracion.<List<String>>json(noviembre, "$[*].tipo")).isEmpty();
    }

    @Test
    void elTipoVacioEsComoNoFiltrar() {
        UUID usuarioId = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        solicitarDebito(usuarioId, UUID.randomUUID(), "150.00");
        esperarEventos("CarteraDebitada");

        // ?tipo= llega como cadena vacía: no es un filtro, no debe rechazarse
        ResponseEntity<String> movimientos = get("/wallet/sistema/movimientos?tipo=", tokenAdminSistema());

        assertThat(movimientos.getStatusCode().value()).as(movimientos.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<List<String>>json(movimientos, "$[*].tipo")).containsExactly("COBRO");
    }

    @Test
    void unTipoQueNoEsCobroNiDevolucionSeRechaza() {
        // RECARGA existe en los movimientos del cliente, pero no en la billetera del sistema
        esError(get("/wallet/sistema/movimientos?tipo=RECARGA", tokenAdminSistema()), 400, "TIPO_INVALIDO");
    }
}
