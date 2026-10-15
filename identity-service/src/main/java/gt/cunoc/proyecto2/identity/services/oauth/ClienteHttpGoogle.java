package gt.cunoc.proyecto2.identity.services.oauth;

import java.util.Map;

/**
 * HTTP mínimo hacia Google (token + JWKS). Interfaz para poder mockearlo en pruebas
 * sin red: la implementación usa RestClient.
 */
public interface ClienteHttpGoogle {

    /** POST x-www-form-urlencoded; lanza RuntimeException si Google no responde 200. */
    Map<String, Object> postFormulario(String url, Map<String, String> formulario);

    /** GET que devuelve el cuerpo como texto; lanza RuntimeException si no es 200. */
    String getTexto(String url);
}
