package ec.edu.praxis.infrastructure.salud;

import ec.edu.praxis.application.puerto.ProveedorEstadoBaseDatos;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@ApplicationScoped
public class ProveedorEstadoBaseDatosPostgres implements ProveedorEstadoBaseDatos {
    @Inject DataSource datos;

    @Override
    public int consultarVersionEsquema() throws Exception {
        try (Connection conexion = datos.getConnection();
             Statement sentencia = conexion.createStatement();
             ResultSet resultado = sentencia.executeQuery(
                     "SELECT max(version::int) FROM flyway_schema_history WHERE success AND version IS NOT NULL")) {
            resultado.next();
            return resultado.getInt(1);
        }
    }
}
