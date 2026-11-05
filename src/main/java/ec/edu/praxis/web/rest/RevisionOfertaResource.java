package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.oferta.RevisarOfertaPracticaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

@Path("/coordinacion/ofertas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("COORDINADOR")
public class RevisionOfertaResource {

    @Inject RevisarOfertaPracticaService service;
    @Inject JsonWebToken jwt;

    private long actor() {
        JsonNumber usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            throw new IllegalStateException("El token no contiene el identificador del usuario.");
        }
        return usuarioId.longValue();
    }

    @GET
    @Path("/pendientes")
    public List<Map<String, Object>> listarPendientes() {
        return service.listarPendientes();
    }

    @GET
    @Path("/{id}")
    public Map<String, Object> detalle(@PathParam("id") long id) {
        return service.detalle(id);
    }

    @PATCH
    @Path("/{id}/aprobar")
    public Map<String, String> aprobar(@PathParam("id") long id) {
        service.aprobar(id, actor());
        return Map.of("mensaje", "La oferta fue aprobada y publicada correctamente.");
    }

    @PATCH
    @Path("/{id}/rechazar")
    public Map<String, String> rechazar(@PathParam("id") long id, Map<String, String> datos) {
        service.rechazar(id, datos == null ? null : datos.get("motivo"), actor());
        return Map.of("mensaje", "La oferta fue rechazada. La empresa podrá corregirla y enviarla nuevamente.");
    }
}
