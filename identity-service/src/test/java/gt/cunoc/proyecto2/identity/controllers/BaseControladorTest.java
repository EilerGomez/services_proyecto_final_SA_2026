package gt.cunoc.proyecto2.identity.controllers;

import gt.cunoc.proyecto2.comun.errores.EscritorErrores;
import gt.cunoc.proyecto2.comun.errores.ManejadorGlobalErrores;
import gt.cunoc.proyecto2.comun.seguridad.config.ConfiguracionSeguridadWeb;
import gt.cunoc.proyecto2.comun.seguridad.filtro.FiltroAutenticacion;
import gt.cunoc.proyecto2.comun.seguridad.filtro.FiltroCorrelacion;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.when;

/**
 * Base de los tests web: pila MVC de comun (filtros, interceptor,
 * resolver y formato de error) + JWT simulado.
 */
@Import({ConfiguracionSeguridadWeb.class, FiltroAutenticacion.class, FiltroCorrelacion.class,
        ManejadorGlobalErrores.class, EscritorErrores.class, BaseControladorTest.Soporte.class})
public abstract class BaseControladorTest {

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected ServicioJwt jwt;

    @TestConfiguration(proxyBeanMethods = false)
    static class Soporte {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }

    protected UsuarioActual usuario(Set<String> roles, UUID sucursalId) {
        return new UsuarioActual(UUID.randomUUID(), "test@correo.com", roles, sucursalId, null);
    }

    protected void sesion(String token, UsuarioActual actual) {
        when(jwt.validar(token)).thenReturn(actual);
    }
}
