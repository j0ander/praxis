package ec.edu.praxis.application.salud;

import ec.edu.praxis.application.puerto.ProveedorEstadoBaseDatos;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConsultarSaludService {
    @Inject ProveedorEstadoBaseDatos proveedor;

    public int versionEsquema() throws Exception {
        return proveedor.consultarVersionEsquema();
    }
}
