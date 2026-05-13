package Exceptions;

public class IncidenteException extends Exception {
    public IncidenteException(String mensaje) {
        super(mensaje);
    }

    public IncidenteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
