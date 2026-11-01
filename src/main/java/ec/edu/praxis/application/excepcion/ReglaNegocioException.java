package ec.edu.praxis.application.excepcion;

/**
 * 422: la petición es válida pero viola una regla de negocio (p. ej. más de 8 h
 * por día).
 */
public class ReglaNegocioException extends PraxisException {
    public ReglaNegocioException(String mensaje) {
        super(422, "REGLA_NEGOCIO", mensaje);
    }
}
