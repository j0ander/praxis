package ec.edu.praxis.application.excepcion;

/**
 * Base de los errores de negocio. Cada subclase fija su código HTTP y un código
 * estable para el frontend.
 */
public abstract class PraxisException extends RuntimeException {

    private final int estado;
    private final String codigo;

    protected PraxisException(int estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public int getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }
}
