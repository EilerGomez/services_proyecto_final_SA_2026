package gt.cunoc.proyecto2.notification.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {

    @Test
    void documentaElServicio() {
        // Act
        var abierto = new OpenApiConfig().notificationOpenApi();

        // Assert
        assertEquals("Notification Service", abierto.getInfo().getTitle());
    }
}
