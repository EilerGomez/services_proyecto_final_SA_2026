package api_gateway.constants;

public final class ApiPaths {
    public static final String IDENTITY = "/identity";
    public static final String FLEET = "/fleet";
    public static final String TRIPS = "/trips";
    public static final String SALES = "/sales";
    public static final String MAINTENANCE = "/maintenance";
    public static final String REPORTS = "/reports";
    public static final String NOTIFICATIONS = "/notifications";
    public static final String HEALTH = "/actuator/health";
    public static final String ACTUATOR = "/actuator/**";
    public static final String SWAGGER_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String WEBJARS = "/webjars/**";
    public static final String API_DOCS = "/v3/api-docs/**";

    private ApiPaths() { }
}
