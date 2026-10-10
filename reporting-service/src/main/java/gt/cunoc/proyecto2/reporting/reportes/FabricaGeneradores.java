package gt.cunoc.proyecto2.reporting.reportes;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FabricaGeneradores {

    private final Map<TipoReporte, GeneradorReporte> generadores;

    public FabricaGeneradores(List<GeneradorReporte> lista) {
        this.generadores = lista.stream()
                .collect(Collectors.toMap(GeneradorReporte::tipo, Function.identity()));
    }

    public GeneradorReporte obtener(TipoReporte tipo) {
        GeneradorReporte generador = generadores.get(tipo);
        if (generador == null) {
            throw new NoEncontradoException("REPORTE_NO_EXISTE", "No existe el generador " + tipo);
        }
        return generador;
    }
}
