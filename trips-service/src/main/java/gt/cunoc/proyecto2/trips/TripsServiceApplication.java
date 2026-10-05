package gt.cunoc.proyecto2.trips;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "gt.cunoc.proyecto2.trips",
        "gt.cunoc.proyecto2.comun"
})
public class TripsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripsServiceApplication.class, args);
    }
}
