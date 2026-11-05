package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.oferta.ConsultarOfertasEstudianteService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

/** Endpoints de consulta de convocatorias para estudiantes autenticados. */
@Path("/estudiante/ofertas")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ESTUDIANTE")
public class OfertaEstudianteResource {

    @Inject ConsultarOfertasEstudianteService service;
    @Inject JsonWebToken jwt;

    private long actor() {
        JsonNumber usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            throw new IllegalStateException("El token no contiene el identificador del usuario.");
        }
        return usuarioId.longValue();
    }

    @GET
    public List<Map<String, Object>> listarPublicadas() {
        return service.listarPublicadas(actor());
    }

    @GET
    @Path("/{id}")
    public Map<String, Object> detalle(@PathParam("id") long id) {
        return service.detallePublicada(id, actor());
    }
}
