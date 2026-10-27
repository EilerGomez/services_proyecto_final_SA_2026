package gt.cunoc.proyecto2.reporting.eventos.manejadores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.reporting.eventos.TipoEvento;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet.ManejadorBusActualizado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet.ManejadorBusCreado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet.ManejadorBusDesactivado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet.ManejadorChoferActualizado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet.ManejadorChoferCreado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet.ManejadorChoferDesactivado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet.ManejadorSucursalActualizada;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.fleet.ManejadorSucursalCreada;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.identity.ManejadorTarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.maintenance.ManejadorCategoriaGastoActualizada;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.maintenance.ManejadorGastoTallerRegistrado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.sales.ManejadorAlquilerPagado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.sales.ManejadorBoletoVendido;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.sales.ManejadorVentaCancelada;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.trips.ManejadorRutaActualizada;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.trips.ManejadorRutaCreada;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.trips.ManejadorRutaEliminada;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.trips.ManejadorViajeActualizado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.trips.ManejadorViajeCompletado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.trips.ManejadorViajeEliminado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.trips.ManejadorViajeIniciado;
import gt.cunoc.proyecto2.reporting.eventos.manejadores.trips.ManejadorViajeProgramado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusDesactivado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferDesactivado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance.CategoriaGastoActualizada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance.GastoTallerRegistrado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.AlquilerPagado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.BoletoVendido;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.VentaCancelada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEliminada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeCompletado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEliminado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeIniciado;
import gt.cunoc.proyecto2.reporting.servicios.ProyeccionReportingServicio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Cada manejador delega en la proyección con el tipo de evento que le corresponde. */
class ManejadoresTest {

    private final ProyeccionReportingServicio proyeccion = mock(ProyeccionReportingServicio.class);

    private static <T> EventoRecibido<T> sobre(String tipo, T payload) {
        return new EventoRecibido<>(UUID.randomUUID(), tipo, 1, UUID.randomUUID(), 1L,
                Instant.now(), LocalDate.of(2026, 10, 20), UUID.randomUUID(), payload);
    }

    private static void verifica(ManejadorEvento<?> manejador, TipoEvento tipo) {
        assertThat(manejador.tipoEvento()).isEqualTo(tipo.nombre());
        assertThat(manejador.tipoPayload()).isNotNull();
    }

    @Test
    void tarifa() {
        var m = new ManejadorTarifaDepreciacionCambiada(proyeccion);
        verifica(m, TipoEvento.TARIFA_DEPRECIACION_CAMBIADA);
        var payload = new TarifaDepreciacionCambiada(UUID.randomUUID(),
                new BigDecimal("1.2500"), LocalDate.of(2026, 10, 1));
        m.manejar(sobre("TarifaDepreciacionCambiada", payload));
        verify(proyeccion).aplicarTarifa(payload);
    }

    @Test
    void sucursales() {
        var payload = new SucursalEvento(UUID.randomUUID(), "N-01", "Norte", "Xela", "Xela",
                new BigDecimal("14.83"), new BigDecimal("-91.51"), true);
        var creada = new ManejadorSucursalCreada(proyeccion);
        verifica(creada, TipoEvento.SUCURSAL_CREADA);
        creada.manejar(sobre("SucursalCreada", payload));
        var actualizada = new ManejadorSucursalActualizada(proyeccion);
        verifica(actualizada, TipoEvento.SUCURSAL_ACTUALIZADA);
        actualizada.manejar(sobre("SucursalActualizada", payload));
        verify(proyeccion, org.mockito.Mockito.times(2)).aplicarSucursal(payload, 1L);
    }

    @Test
    void buses() {
        var payload = new BusEvento(UUID.randomUUID(), UUID.randomUUID(), "C123ABC", "Volvo", "B12",
                50, "ACTIVO", new BigDecimal("1000"));
        var creado = new ManejadorBusCreado(proyeccion);
        verifica(creado, TipoEvento.BUS_CREADO);
        creado.manejar(sobre("BusCreado", payload));
        var actualizado = new ManejadorBusActualizado(proyeccion);
        verifica(actualizado, TipoEvento.BUS_ACTUALIZADO);
        actualizado.manejar(sobre("BusActualizado", payload));
        verify(proyeccion, org.mockito.Mockito.times(2)).aplicarBus(payload, 1L);
        var desactivado = new ManejadorBusDesactivado(proyeccion);
        verifica(desactivado, TipoEvento.BUS_DESACTIVADO);
        var off = new BusDesactivado(payload.busId());
        desactivado.manejar(sobre("BusDesactivado", off));
        verify(proyeccion).desactivarBus(payload.busId(), 1L);
        desactivado.manejar(sobre("BusDesactivado", new BusDesactivado(null)));
    }

    @Test
    void choferes() {
        var payload = new ChoferEvento(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Juan Pérez", "LIC-1", "A", LocalDate.of(2027, 1, 1),
                new BigDecimal("350"), "ACTIVO");
        var creado = new ManejadorChoferCreado(proyeccion);
        verifica(creado, TipoEvento.CHOFER_CREADO);
        creado.manejar(sobre("ChoferCreado", payload));
        var actualizado = new ManejadorChoferActualizado(proyeccion);
        verifica(actualizado, TipoEvento.CHOFER_ACTUALIZADO);
        actualizado.manejar(sobre("ChoferActualizado", payload));
        verify(proyeccion, org.mockito.Mockito.times(2)).aplicarChofer(payload, 1L);
        var desactivado = new ManejadorChoferDesactivado(proyeccion);
        verifica(desactivado, TipoEvento.CHOFER_DESACTIVADO);
        var off = new ChoferDesactivado(payload.choferId());
        desactivado.manejar(sobre("ChoferDesactivado", off));
        verify(proyeccion).desactivarChofer(payload.choferId(), 1L);
        desactivado.manejar(sobre("ChoferDesactivado", new ChoferDesactivado(null)));
    }

    @Test
    void rutas() {
        var payload = new RutaEvento(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Xela", "Guate", new BigDecimal("205"), new BigDecimal("75"), true);
        var creada = new ManejadorRutaCreada(proyeccion);
        verifica(creada, TipoEvento.RUTA_CREADA);
        creada.manejar(sobre("RutaCreada", payload));
        var actualizada = new ManejadorRutaActualizada(proyeccion);
        verifica(actualizada, TipoEvento.RUTA_ACTUALIZADA);
        actualizada.manejar(sobre("RutaActualizada", payload));
        verify(proyeccion, org.mockito.Mockito.times(2)).aplicarRuta(payload);
        var eliminada = new ManejadorRutaEliminada(proyeccion);
        verifica(eliminada, TipoEvento.RUTA_ELIMINADA);
        var off = new RutaEliminada(payload.rutaId());
        eliminada.manejar(sobre("RutaEliminada", off));
        verify(proyeccion).eliminarRuta(payload.rutaId());
        var eliminadaNula = new ManejadorRutaEliminada(proyeccion);
        eliminadaNula.manejar(sobre("RutaEliminada", new RutaEliminada(null)));
    }

    @Test
    void viajes() {
        var payload = new ViajeEvento(UUID.randomUUID(), "REGULAR", UUID.randomUUID(), null,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.of(2026, 10, 20, 8, 0, 0, 0, ZoneOffset.UTC));
        var programado = new ManejadorViajeProgramado(proyeccion);
        verifica(programado, TipoEvento.VIAJE_PROGRAMADO);
        programado.manejar(sobre("ViajeProgramado", payload));
        verify(proyeccion).aplicarViaje(payload, 1L);
        var actualizado = new ManejadorViajeActualizado(proyeccion);
        verifica(actualizado, TipoEvento.VIAJE_ACTUALIZADO);
        actualizado.manejar(sobre("ViajeActualizado", payload));
        verify(proyeccion).aplicarViajeActualizado(payload, 1L);
        var eliminado = new ManejadorViajeEliminado(proyeccion);
        verifica(eliminado, TipoEvento.VIAJE_ELIMINADO);
        var del = new ViajeEliminado(payload.viajeId());
        eliminado.manejar(sobre("ViajeEliminado", del));
        verify(proyeccion).eliminarViaje(payload.viajeId());
        var eliminadoNulo = new ManejadorViajeEliminado(proyeccion);
        eliminadoNulo.manejar(sobre("ViajeEliminado", new ViajeEliminado(null)));
        var iniciado = new ManejadorViajeIniciado(proyeccion);
        verifica(iniciado, TipoEvento.VIAJE_INICIADO);
        var ini = new ViajeIniciado(payload.viajeId(), payload.busId(), payload.choferId());
        iniciado.manejar(sobre("ViajeIniciado", ini));
        verify(proyeccion).marcarViajeEnRuta(payload.viajeId(), payload.busId(), 1L);
        var iniciadoNulo = new ManejadorViajeIniciado(proyeccion);
        iniciadoNulo.manejar(sobre("ViajeIniciado",
                new ViajeIniciado(null, payload.busId(), payload.choferId())));
        var completado = new ManejadorViajeCompletado(proyeccion);
        verifica(completado, TipoEvento.VIAJE_COMPLETADO);
        var fin = new ViajeCompletado(payload.viajeId(), "REGULAR", payload.rutaId(),
                payload.sucursalId(), payload.busId(), payload.choferId(), new BigDecimal("50"),
                new BigDecimal("200"), new BigDecimal("1.5"), new BigDecimal("75"),
                new BigDecimal("120"), OffsetDateTime.now());
        var evento = sobre("ViajeCompletado", fin);
        completado.manejar(evento);
        verify(proyeccion).aplicarViajeCompletado(
                evento.eventId(), fin, 1L, LocalDate.of(2026, 10, 20));
    }

    @Test
    void ventas() {
        var boleto = new BoletoVendido(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), null, "CF", "CF", 2,
                new BigDecimal("150"), LocalDate.of(2026, 10, 20), "Xela", "Guate", null);
        var vendido = new ManejadorBoletoVendido(proyeccion);
        verifica(vendido, TipoEvento.BOLETO_VENDIDO);
        var eventoBoleto = sobre("BoletoVendido", boleto);
        vendido.manejar(eventoBoleto);
        verify(proyeccion).aplicarBoletoVendido(
                eventoBoleto.eventId(), boleto, LocalDate.of(2026, 10, 20));
        var cancelada = new ManejadorVentaCancelada(proyeccion);
        verifica(cancelada, TipoEvento.VENTA_CANCELADA);
        var cancel = new VentaCancelada(boleto.ventaId(), boleto.viajeId(), boleto.sucursalId(),
                "CF", "CF", 2, new BigDecimal("150"));
        var eventoCancel = sobre("VentaCancelada", cancel);
        cancelada.manejar(eventoCancel);
        verify(proyeccion).aplicarVentaCancelada(
                eventoCancel.eventId(), cancel, LocalDate.of(2026, 10, 20));
        var alquiler = new ManejadorAlquilerPagado(proyeccion);
        verifica(alquiler, TipoEvento.ALQUILER_PAGADO);
        var alq = new AlquilerPagado(UUID.randomUUID(), null, boleto.sucursalId(), "CF", "CF",
                "Xela", "Guate", null, null, new BigDecimal("4300"), null, LocalDate.of(2026, 10, 20));
        var eventoAlq = sobre("AlquilerPagado", alq);
        alquiler.manejar(eventoAlq);
        verify(proyeccion).aplicarAlquilerPagado(
                eventoAlq.eventId(), alq, LocalDate.of(2026, 10, 20));
    }

    @Test
    void taller() {
        var taller = new ManejadorGastoTallerRegistrado(proyeccion);
        verifica(taller, TipoEvento.GASTO_TALLER_REGISTRADO);
        var linea = new GastoTallerRegistrado.Linea(UUID.randomUUID(), UUID.randomUUID(),
                "Llantas", "Cambio", "REPUESTO", new BigDecimal("100"));
        var gasto = new GastoTallerRegistrado(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDate.of(2026, 10, 20), List.of(linea));
        var eventoGasto = sobre("GastoTallerRegistrado", gasto);
        taller.manejar(eventoGasto);
        verify(proyeccion).aplicarGastoTaller(
                eventoGasto.eventId(), gasto, LocalDate.of(2026, 10, 20));
        var categoria = new ManejadorCategoriaGastoActualizada(proyeccion);
        verifica(categoria, TipoEvento.CATEGORIA_GASTO_ACTUALIZADA);
        var cat = new CategoriaGastoActualizada(UUID.randomUUID(), "Motor", true);
        categoria.manejar(sobre("CategoriaGastoActualizada", cat));
        verify(proyeccion).aplicarCategoriaActualizada(cat.categoriaId(), "Motor");
    }
}
