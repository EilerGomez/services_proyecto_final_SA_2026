package gt.cunoc.proyecto2.sales.tareas;

import gt.cunoc.proyecto2.sales.servicios.ventas.VentaServicio;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TareaLiberarReservas {

    private final VentaServicio ventas;

    public TareaLiberarReservas(VentaServicio ventas) {
        this.ventas = ventas;
    }

    @Scheduled(fixedDelayString = "${app.ventas.liberar-reservas-ms:30000}",
            initialDelayString = "${app.ventas.liberar-reservas-ms:30000}")
    public void ejecutar() {
        ventas.liberarReservasVencidas();
    }
}