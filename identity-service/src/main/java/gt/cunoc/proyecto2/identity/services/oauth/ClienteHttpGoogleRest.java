package gt.cunoc.proyecto2.identity.services.oauth;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class ClienteHttpGoogleRest implements ClienteHttpGoogle {

    private final RestClient rest;

    public ClienteHttpGoogleRest(RestClient.Builder builder) {
        this.rest = builder.build();
    }

    @Override
    public Map<String, Object> postFormulario(String url, Map<String, String> formulario) {
        LinkedMultiValueMap<String, String> cuerpo = new LinkedMultiValueMap<>();
        formulario.forEach(cuerpo::add);
        Map<?, ?> respuesta = rest.post().uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(cuerpo)
                .retrieve()
                .body(Map.class);
        if (respuesta == null) {
            throw new IllegalStateException("Google no devolvió cuerpo");
        }
        return (Map<String, Object>) respuesta;
    }

    @Override
    public String getTexto(String url) {
        String cuerpo = rest.get().uri(url).retrieve().body(String.class);
        if (cuerpo == null) {
            throw new IllegalStateException("Google no devolvió cuerpo");
        }
        return cuerpo;
    }
}
