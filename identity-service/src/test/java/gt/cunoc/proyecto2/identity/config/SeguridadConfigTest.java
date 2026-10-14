package gt.cunoc.proyecto2.identity.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SeguridadConfig: clase sin cubrir (bean BCrypt y cadena de filtros permisiva). */
class SeguridadConfigTest {

    private final SeguridadConfig configuracion = new SeguridadConfig();

    @Test
    void codificadorVerificaClaves() {
        var codificador = configuracion.codificadorClaves();

        assertThat(codificador.matches("Secreta#2026", codificador.encode("Secreta#2026"))).isTrue();
    }

    @Test
    void cadenaFiltrosPermiteTodo() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        when(http.csrf(any())).thenReturn(http);
        when(http.httpBasic(any())).thenReturn(http);
        when(http.formLogin(any())).thenReturn(http);
        when(http.logout(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        DefaultSecurityFilterChain cadena = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(cadena);

        assertThat(configuracion.cadenaFiltros(http)).isSameAs(cadena);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void personalizacionDesactivaFiltrosYPermiteTodo() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        when(http.csrf(any())).thenReturn(http);
        when(http.httpBasic(any())).thenReturn(http);
        when(http.formLogin(any())).thenReturn(http);
        when(http.logout(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        configuracion.cadenaFiltros(http);

        ArgumentCaptor<Customizer> csrf = ArgumentCaptor.forClass(Customizer.class);
        verify(http).csrf(csrf.capture());
        ((Customizer<CsrfConfigurer<HttpSecurity>>) csrf.getValue())
                .customize(mock(CsrfConfigurer.class, RETURNS_DEEP_STUBS));

        ArgumentCaptor<Customizer> basica = ArgumentCaptor.forClass(Customizer.class);
        verify(http).httpBasic(basica.capture());
        ((Customizer<HttpBasicConfigurer<HttpSecurity>>) basica.getValue())
                .customize(mock(HttpBasicConfigurer.class, RETURNS_DEEP_STUBS));

        ArgumentCaptor<Customizer> formulario = ArgumentCaptor.forClass(Customizer.class);
        verify(http).formLogin(formulario.capture());
        ((Customizer<FormLoginConfigurer<HttpSecurity>>) formulario.getValue())
                .customize(mock(FormLoginConfigurer.class, RETURNS_DEEP_STUBS));

        ArgumentCaptor<Customizer> salida = ArgumentCaptor.forClass(Customizer.class);
        verify(http).logout(salida.capture());
        ((Customizer<LogoutConfigurer<HttpSecurity>>) salida.getValue())
                .customize(mock(LogoutConfigurer.class, RETURNS_DEEP_STUBS));

        ArgumentCaptor<Customizer> autorizacion = ArgumentCaptor.forClass(Customizer.class);
        verify(http).authorizeHttpRequests(autorizacion.capture());
        ((Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>
                .AuthorizationManagerRequestMatcherRegistry>) autorizacion.getValue())
                .customize(mock(AuthorizeHttpRequestsConfigurer
                        .AuthorizationManagerRequestMatcherRegistry.class, RETURNS_DEEP_STUBS));
    }
}
