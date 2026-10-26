package gt.cunoc.proyecto2.sales.servicios.clientes;

import gt.cunoc.proyecto2.sales.dtos.clientes.ClienteRespuesta;
import java.util.List;

/** Consulta de clientes con cuenta para la ventanilla (S4: vender a nombre de un cliente del sistema). */
public interface ClienteServicio {

    /**
     * Clientes cuyo NIT, nombre o correo contienen el texto. Sin texto devuelve los primeros en
     * orden alfabético, para que el cajero pueda elegir sin teclear nada. Nunca más de los primeros
     * resultados: es un buscador de ventanilla, no un listado de todos los clientes.
     */
    List<ClienteRespuesta> buscar(String texto);
}
