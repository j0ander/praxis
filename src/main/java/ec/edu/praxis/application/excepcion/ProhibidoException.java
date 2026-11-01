package ec.edu.praxis.application.excepcion;

/** 403: autenticado pero sin permiso sobre el recurso. */
public class ProhibidoException extends PraxisException {
    public ProhibidoException(String mensaje) {
        super(403, "PROHIBIDO", mensaje);
    }
}
