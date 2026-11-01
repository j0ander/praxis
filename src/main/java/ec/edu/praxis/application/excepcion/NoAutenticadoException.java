package ec.edu.praxis.application.excepcion;

public class NoAutenticadoException extends PraxisException {
    public NoAutenticadoException() {
        super(401, "NO_AUTENTICADO", "Credenciales incorrectas o sesión no válida.");
    }
}
