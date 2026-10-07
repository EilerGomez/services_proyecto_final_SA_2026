/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.errores.sucursales;

/**
 *
 * @author eiler
 */
import gt.cunoc.proyecto2.fleet.errores.CodigoError;
import gt.cunoc.proyecto2.fleet.errores.TipoError;
public enum ErrorSucursal implements CodigoError {
 
    SUCURSAL_NO_EXISTE(TipoError.NO_ENCONTRADO, "La sucursal no existe"),
    SUCURSAL_DUPLICADA(TipoError.CONFLICTO, "Ya existe una sucursal con el %s %s"),
    SUCURSAL_INACTIVA(TipoError.REGLA_NEGOCIO, "La sucursal está inactiva");
 
    private final TipoError tipo;
    private final String mensaje;
 
    ErrorSucursal(TipoError tipo, String mensaje) {
        this.tipo = tipo;
        this.mensaje = mensaje;
    }
 
    @Override
    public TipoError tipo() {
        return tipo;
    }
 
    @Override
    public String mensaje() {
        return mensaje;
    }
}
 
