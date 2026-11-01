package ec.edu.praxis.application.puerto;

/** Puerto para consultar el estado del almacenamiento sin acoplar application a JDBC. */
public interface ProveedorEstadoBaseDatos {
    int consultarVersionEsquema() throws Exception;
}
