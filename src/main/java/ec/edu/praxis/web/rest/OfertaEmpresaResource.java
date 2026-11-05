package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.oferta.GestionarOfertaPracticaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

@Path("/empresa/ofertas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("REPRESENTANTE_EMPRESA")
public class OfertaEmpresaResource {

    @Inject GestionarOfertaPracticaService service;
    @Inject JsonWebToken jwt;

    private long actor() {
        JsonNumber usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            throw new IllegalStateException("El token no contiene el identificador del usuario.");
        }
        return usuarioId.longValue();
    }

    @GET
    public List<Map<String, Object>> listar() {
        return service.listar(actor());
    }

    @GET
    @Path("/opciones")
    public Map<String, Object> opciones() {
        return service.opciones(actor());
    }

    @POST
    public Map<String, Object> crear(Map<String, Object> datos) {
        long id = service.crear(datos, actor());
        return Map.of("id", id, "mensaje", "Oferta guardada como borrador.");
    }

    @PUT
    @Path("/{id}")
    public Map<String, String> actualizar(@PathParam("id") long id, Map<String, Object> datos) {
        service.actualizar(id, datos, actor());
        return Map.of("mensaje", "La oferta fue actualizada correctamente.");
    }

    @PATCH
    @Path("/{id}/enviar-revision")
    public Map<String, String> enviarRevision(@PathParam("id") long id) {
        service.enviarRevision(id, actor());
        return Map.of("mensaje", "La oferta fue enviada a revisión.");
    }
}
