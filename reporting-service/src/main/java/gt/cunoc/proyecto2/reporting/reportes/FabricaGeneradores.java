package gt.cunoc.proyecto2.reporting.reportes;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Resuelve qué generador produce cada tipo de reporte.
 *
 * <p>Al arrancar recibe todos los generadores registrados y los indexa por su tipo; así los
 * controladores piden un reporte por su enumeración sin conocer las clases concretas. Si el tipo no
 * tiene generador responde 404.
 */
@Component
public class FabricaGeneradores {

    // Mapa de tipo de reporte a la clase que lo genera.
    private final Map<TipoReporte, GeneradorReporte> generadores;

    // Indexa los generadores disponibles por el tipo que declara cada uno.
    public FabricaGeneradores(List<GeneradorReporte> lista) {
        this.generadores = lista.stream()
                .collect(Collectors.toMap(GeneradorReporte::tipo, Function.identity()));
    }

    // Devuelve el generador del tipo pedido o falla con 404 si no existe.
    public GeneradorReporte obtener(TipoReporte tipo) {
        GeneradorReporte generador = generadores.get(tipo);
        if (generador == null) {
            throw new NoEncontradoException("REPORTE_NO_EXISTE", "No existe el generador " + tipo);
        }
        return generador;
    }
}
