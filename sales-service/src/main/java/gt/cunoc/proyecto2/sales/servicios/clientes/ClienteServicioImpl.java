package gt.cunoc.proyecto2.sales.servicios.clientes;

import gt.cunoc.proyecto2.sales.dtos.clientes.ClienteRespuesta;
import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.utilidades.Normalizador;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServicioImpl implements ClienteServicio {

    /** Cuántos clientes ve la ventanilla de una vez: suficiente para elegir, sin volcar la tabla. */
    private static final int MAXIMO = 20;

    private final VistaClienteRepositorio clientes;

    public ClienteServicioImpl(VistaClienteRepositorio clientes) {
        this.clientes = clientes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteRespuesta> buscar(String texto) {
        Pageable primeros = PageRequest.of(0, MAXIMO);
        String limpio = Normalizador.textoOpcional(texto);
        List<VistaClienteEntidad> encontrados = limpio == null
                ? clientes.findAllByOrderByNombreAsc(primeros)
                : clientes.buscar("%" + limpio.toUpperCase(Locale.ROOT) + "%", primeros);
        return encontrados.stream().map(ClienteRespuesta::new).toList();
    }
}
