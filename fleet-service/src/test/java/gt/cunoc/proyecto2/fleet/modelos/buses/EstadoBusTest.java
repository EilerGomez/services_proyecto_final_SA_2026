package gt.cunoc.proyecto2.fleet.modelos.buses;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoBusTest {

    @Test
    void soloEnRutaNoSeAsignaManualmente() {
        assertThat(EstadoBus.ACTIVO.seAsignaManualmente()).isTrue();
        assertThat(EstadoBus.EN_TALLER.seAsignaManualmente()).isTrue();
        assertThat(EstadoBus.INACTIVO.seAsignaManualmente()).isTrue();
        assertThat(EstadoBus.EN_RUTA.seAsignaManualmente()).isFalse();
    }

    @Test
    void activoYEnRutaEstanDisponiblesParaViajes() {
        assertThat(EstadoBus.ACTIVO.disponibleParaViajes()).isTrue();
        assertThat(EstadoBus.EN_RUTA.disponibleParaViajes()).isTrue();
        assertThat(EstadoBus.EN_TALLER.disponibleParaViajes()).isFalse();
        assertThat(EstadoBus.INACTIVO.disponibleParaViajes()).isFalse();
    }
}
