package api_gateway.revocaciones;

import api_gateway.constants.ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rechaza el JWT si se emitió antes de que retiraran o desactivaran a la persona. Va junto a los
 * validadores de firma y vigencia: el token llega como inválido y el gateway responde 401 SESION_REVOCADA.
 */
public class TokenNoRevocadoValidator implements OAuth2TokenValidator<Jwt> {

    public static final String MENSAJE = "Tu acceso cambió; vuelve a iniciar sesión";

    static final OAuth2Error ERROR = new OAuth2Error(ErrorCodes.SESION_REVOCADA, MENSAJE, null);

    private final RegistroRevocaciones registro;

    public TokenNoRevocadoValidator(RegistroRevocaciones registro) {
        this.registro = registro;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return registro.revocado(token.getSubject(), token.getIssuedAt())
                ? OAuth2TokenValidatorResult.failure(ERROR)
                : OAuth2TokenValidatorResult.success();
    }
}
