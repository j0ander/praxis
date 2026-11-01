package ec.edu.praxis.application.excepcion;

/** 409: choca con el estado actual (duplicado, ya existe, etc.). */
public class ConflictoException extends PraxisException {
    public ConflictoException(String mensaje) {
        super(409, "CONFLICTO", mensaje);
    }
}
