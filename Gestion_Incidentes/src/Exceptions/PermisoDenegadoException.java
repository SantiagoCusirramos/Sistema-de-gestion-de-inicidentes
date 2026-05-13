package Exceptions;

public class PermisoDenegadoException extends Exception {
    public PermisoDenegadoException(String mensaje) {
        super(mensaje);
    }

    public PermisoDenegadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
