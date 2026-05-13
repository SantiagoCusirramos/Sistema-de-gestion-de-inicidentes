package Exceptions;

public class BaseDeDatosException extends Exception {
    public BaseDeDatosException(String mensaje) {
        super(mensaje);
    }

    public BaseDeDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
