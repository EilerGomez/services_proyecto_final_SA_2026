package gt.cunoc.proyecto2.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Wallet Service: cartera digital de los clientes.
 *
 * <p>Se separó de Identity porque el dinero es otro contexto de negocio: otras reglas de auditoría,
 * otra seguridad (las pasarelas de pago solo tocan este servicio) y otra escala que el inicio de sesión.
 * Habla con los demás por eventos (saga con Sales) y solo llama a Identity para conocer el estado de la cuenta.
 */
@SpringBootApplication(scanBasePackages = {
        "gt.cunoc.proyecto2.wallet",
        "gt.cunoc.proyecto2.comun"
})
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}
