package ec.edu.praxis.application.excepcion;

/** 404: el recurso no existe. */
public class NoEncontradoException extends PraxisException {
    public NoEncontradoException(String mensaje) {
        super(404, "NO_ENCONTRADO", mensaje);
    }
}
