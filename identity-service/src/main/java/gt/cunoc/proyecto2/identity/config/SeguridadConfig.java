package gt.cunoc.proyecto2.identity.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


/**
 * Seguridad del servicio Identity.
 *
 * <p>La autenticación y autorización de endpoints la hace {@code comun}
 * ({@code FiltroAutenticacion} + {@code @RequiereRol}); por eso esta cadena
 * permite todo y solo aporta el {@link PasswordEncoder} BCrypt para el inicio de sesión.
 */
@Configuration
public class SeguridadConfig {

    @Bean
    public PasswordEncoder codificadorClaves() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadenaFiltros(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
