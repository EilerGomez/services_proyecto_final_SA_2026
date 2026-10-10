package gt.cunoc.proyecto2.sales.dtos.ventas;

import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import java.util.HashSet;
import java.util.List;

public final class Asientos {

    private Asientos() {
    }

    public static List<Integer> validar(List<Integer> asientos, int capacidad) {
        if (new HashSet<>(asientos).size() != asientos.size()) {
            throw ErrorVenta.ASIENTOS_REPETIDOS.excepcion();
        }
        for (Integer numero : asientos) {
            if (numero > capacidad) {
                throw ErrorVenta.ASIENTO_FUERA_DE_RANGO.excepcion(capacidad, numero);
            }
        }
        return asientos.stream().sorted().toList();
    }
}