package gt.cunoc.proyecto2.fleet.modelos.fotos;

public enum TipoFoto {

    BUS("buses"),
    CHOFER("choferes");

    private final String carpeta;

    TipoFoto(String carpeta) {
        this.carpeta = carpeta;
    }

    public String carpeta() {
        return carpeta;
    }
}