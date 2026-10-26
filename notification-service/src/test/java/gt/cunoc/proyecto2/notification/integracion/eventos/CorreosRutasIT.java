package gt.cunoc.proyecto2.notification.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.notification.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Correos de Trips: aviso a los administradores cuando cambia una ruta. */
class CorreosRutasIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(45);

    @Test
    void rutaActualizadaAvisaALosAdministradoresSinRepetir() {
        // El Identity de prueba devuelve el mismo administrador para cualquier sucursal: el aviso
        // debe llegar una sola vez aunque la ruta toque dos sucursales.
        String payload = """
                {"rutaId":"%s","sucursalOrigenId":"%s","sucursalDestinoId":"%s",
                 "origenNombre":"Quetzaltenango","destinoNombre":"Guatemala",
                 "distanciaKm":205.5,"precioBoleto":75.00,"activa":true}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        UUID eventId = publicarEvento(TRIPS_EVENTOS, "RutaActualizada", "2026-11-01", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(asuntosPara(ADMIN_CORREO))
                .containsExactly("Cambió la ruta Quetzaltenango → Guatemala"));
        assertThat(textoPara(ADMIN_CORREO)).contains(
                "Distancia actual: 205.5 km",
                "Precio actual del boleto: Q 75.00",
                "Estado: activa",
                "conservan el precio");
        assertThat(notificaciones(eventId)).hasSize(1);
        assertThat(notificaciones(eventId).get(0))
                .containsEntry("plantilla", "RUTA_ACTUALIZADA")
                .containsEntry("estado", "ENVIADA");
    }
}
