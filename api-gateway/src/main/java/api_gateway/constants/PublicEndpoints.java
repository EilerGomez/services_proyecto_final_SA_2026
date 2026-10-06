package api_gateway.constants;

public final class PublicEndpoints {
    public static final String[] PATHS = {
        ApiPaths.HEALTH, ApiPaths.SWAGGER_HTML, ApiPaths.SWAGGER_UI,
        ApiPaths.WEBJARS, ApiPaths.API_DOCS
    };

    private PublicEndpoints() { }
}
