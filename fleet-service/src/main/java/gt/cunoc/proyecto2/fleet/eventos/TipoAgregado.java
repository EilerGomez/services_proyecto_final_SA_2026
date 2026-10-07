package gt.cunoc.proyecto2.fleet.eventos;

public enum TipoAgregado {

    SUCURSAL("Sucursal"),
    PERSONAL_SUCURSAL("PersonalSucursal"),
    BUS("Bus"),
    CHOFER("Chofer");

    private final String nombre;

    TipoAgregado(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}