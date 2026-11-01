package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.seguridad.AdministrarSeguridadService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.json.JsonNumber;
import java.util.*;

@Path("/admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
public class AdministracionResource {
    @Inject
    AdministrarSeguridadService service;
    @Inject
    JsonWebToken jwt;

    private long actor() {
        JsonNumber usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            throw new IllegalStateException("El token no contiene el identificador del usuario.");
        }
        return usuarioId.longValue();
    }

    private Map<String, Object> creado(long id) {
        return Map.of("id", id, "mensaje", "Registro creado correctamente.");
    }

    @GET
    @Path("/roles")
    public List<?> roles() {
        return service.roles();
    }

    @GET
    @Path("/usuarios")
    public List<?> usuarios() {
        return service.listarUsuarios();
    }

    @POST
    @Path("/usuarios")
    public Map<String, Object> crearUsuario(Map<String, Object> d) {
        return creado(service.crearUsuario(d, actor()));
    }

    @PATCH
    @Path("/usuarios/{id}/estado")
    public Map<String, String> estadoUsuario(@PathParam("id") long id, Map<String, Boolean> d) {
        service.estadoUsuario(id, Boolean.TRUE.equals(d.get("activo")), actor());
        return Map.of("mensaje", "Estado actualizado.");
    }

    @PUT
    @Path("/usuarios/{id}/roles")
    public Map<String, String> rolesUsuario(@PathParam("id") long id, Map<String, List<String>> d) {
        service.asignarRoles(id, d.getOrDefault("roles", List.of()), actor());
        return Map.of("mensaje", "Roles actualizados.");
    }

    @PUT
    @Path("/usuarios/{id}/carreras")
    public Map<String, String> carrerasUsuario(@PathParam("id") long id, Map<String, Object> d) {
        List<Long> ids = ((List<?>) d.getOrDefault("carreras", List.of())).stream().map(x -> ((Number) x).longValue())
                .toList();
        service.asignarCarreras(id, ids, Boolean.TRUE.equals(d.get("alcanceGlobal")), actor());
        return Map.of("mensaje", "Alcance actualizado.");
    }

    @POST
    @Path("/usuarios/{id}/restablecer-password")
    public Map<String, String> restablecer(@PathParam("id") long id, Map<String, String> d) {
        service.restablecerPassword(id, d.get("passwordTemporal"), actor());
        return Map.of("mensaje", "Contraseña restablecida. El usuario deberá cambiarla.");
    }

    @GET
    @Path("/facultades")
    public List<?> facultades() {
        return service.facultades();
    }

    @POST
    @Path("/facultades")
    public Map<String, Object> crearFacultad(Map<String, Object> d) {
        return creado(service.crearFacultad(d, actor()));
    }

    @PATCH
    @Path("/facultades/{id}")
    public Map<String, String> desactivarFacultad(@PathParam("id") long id) {
        service.desactivar("facultad", id, actor());
        return Map.of("mensaje", "Facultad desactivada.");
    }

    @GET
    @Path("/carreras")
    public List<?> carreras() {
        return service.carreras();
    }

    @POST
    @Path("/carreras")
    public Map<String, Object> crearCarrera(Map<String, Object> d) {
        return creado(service.crearCarrera(d, actor()));
    }

    @PATCH
    @Path("/carreras/{id}")
    public Map<String, String> desactivarCarrera(@PathParam("id") long id) {
        service.desactivar("carrera", id, actor());
        return Map.of("mensaje", "Carrera desactivada.");
    }

    @GET
    @Path("/periodos")
    public List<?> periodos() {
        return service.periodos();
    }

    @GET
    @Path("/periodos/activo")

    public List<?> periodoActivo() {
        return service.periodoActivo();
    }

    @POST
    @Path("/periodos")
    public Map<String, Object> crearPeriodo(Map<String, Object> d) {
        return creado(service.crearPeriodo(d, actor()));
    }

    @POST
    @Path("/periodos/{id}/activar")
    public Map<String, String> activarPeriodo(@PathParam("id") long id) {
        service.activarPeriodo(id, actor());
        return Map.of("mensaje", "Periodo activado.");
    }

    @GET
    @Path("/auditoria")
    public List<?> auditoria(@QueryParam("limite") @DefaultValue("50") int limite) {
        return service.auditoria(limite);
    }
}
