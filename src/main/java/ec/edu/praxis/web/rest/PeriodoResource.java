package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.seguridad.AdministrarSeguridadService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/periodos")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class PeriodoResource {
    @Inject
    AdministrarSeguridadService service;

    @GET
    @Path("/activo")
    public List<?> activo() {
        return service.periodoActivo();
    }
}
