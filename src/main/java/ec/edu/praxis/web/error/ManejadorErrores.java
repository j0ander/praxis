package ec.edu.praxis.web.error;

import ec.edu.praxis.application.excepcion.PraxisException;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

/**
 * Traduce toda excepción al formato {@link ErrorRespuesta}.
 * 400 validación · 403/404/409/422 negocio · 409/422 desde la BD · 500 resto
 * (sin filtrar detalles internos).
 */
public class ManejadorErrores {

    private static final Logger LOG = Logger.getLogger(ManejadorErrores.class);

    @ServerExceptionMapper
    public Response negocio(PraxisException e, ContainerRequestContext req) {
        return responder(ErrorRespuesta.de(e.getEstado(), e.getCodigo(), e.getMessage(), FiltroCorrelacion.de(req)));
    }

    @ServerExceptionMapper
    public Response validacion(ConstraintViolationException e, ContainerRequestContext req) {
        List<ErrorRespuesta.Detalle> detalles = e.getConstraintViolations().stream()
                .map(ManejadorErrores::detalle)
                .sorted(Comparator.comparing(ErrorRespuesta.Detalle::campo))
                .toList();
        return responder(ErrorRespuesta.de(400, "VALIDACION", "Hay datos inválidos en la petición.",
                detalles, FiltroCorrelacion.de(req)));
    }

    @ServerExceptionMapper
    public Response argumentoInvalido(IllegalArgumentException e, ContainerRequestContext req) {
        return responder(ErrorRespuesta.de(400, "PETICION_INVALIDA", e.getMessage(), FiltroCorrelacion.de(req)));
    }

    @ServerExceptionMapper
    public Response noEncontrado(NotFoundException e, ContainerRequestContext req) {
        return responder(ErrorRespuesta.de(404, "NO_ENCONTRADO", "El recurso solicitado no existe.",
                FiltroCorrelacion.de(req)));
    }

    @ServerExceptionMapper
    public Response web(WebApplicationException e, ContainerRequestContext req) {
        int estado = e.getResponse().getStatus();
        if (estado >= 500) {
            return generico(e, req);
        }
        String codigo = switch (estado) {
            case 401 -> "NO_AUTENTICADO";
            case 403 -> "PROHIBIDO";
            case 404 -> "NO_ENCONTRADO";
            case 405 -> "METODO_NO_PERMITIDO";
            case 415 -> "TIPO_NO_SOPORTADO";
            default -> "PETICION_INVALIDA";
        };
        return responder(ErrorRespuesta.de(estado, codigo, mensajeSeguro(estado), FiltroCorrelacion.de(req)));
    }

    /**
     * Errores que vienen de PostgreSQL (triggers del esquema, únicos, claves
     * foráneas).
     */
    @ServerExceptionMapper
    public Response persistencia(PersistenceException e, ContainerRequestContext req) {
        SQLException sql = buscarSql(e);
        String id = FiltroCorrelacion.de(req);
        if (sql != null) {
            String estado = sql.getSQLState();
            if ("P0001".equals(estado) || "23514".equals(estado) || "23502".equals(estado)) { // triggers y
                                                                                              // restricciones CHECK/NOT
                                                                                              // NULL
                return responder(ErrorRespuesta.de(422, "REGLA_NEGOCIO", primeraLinea(sql.getMessage()), id));
            }
            if ("23505".equals(estado)) {
                return responder(ErrorRespuesta.de(409, "CONFLICTO", "Ya existe un registro con esos datos.", id));
            }
            if ("23503".equals(estado)) {
                return responder(ErrorRespuesta.de(409, "CONFLICTO",
                        "La operación referencia o depende de otro registro.", id));
            }
        }
        return generico(e, req);
    }

    @ServerExceptionMapper
    public Response inesperado(Throwable e, ContainerRequestContext req) {
        return generico(e, req);
    }

    // ---- utilidades ----

    private Response generico(Throwable e, ContainerRequestContext req) {
        String id = FiltroCorrelacion.de(req);
        LOG.errorf(e, "Error no controlado [%s]", id);
        return responder(ErrorRespuesta.de(500, "ERROR_INTERNO",
                "Ocurrió un error interno. Indique el código de correlación al soporte.", id));
    }

    private static Response responder(ErrorRespuesta cuerpo) {
        return Response.status(cuerpo.estado()).type(MediaType.APPLICATION_JSON).entity(cuerpo).build();
    }

    private static ErrorRespuesta.Detalle detalle(ConstraintViolation<?> v) {
        String ruta = v.getPropertyPath().toString();
        int punto = ruta.lastIndexOf('.');
        return new ErrorRespuesta.Detalle(punto >= 0 ? ruta.substring(punto + 1) : ruta, v.getMessage());
    }

    private static SQLException buscarSql(Throwable t) {
        for (Throwable c = t; c != null && c.getCause() != c; c = c.getCause()) {
            if (c instanceof SQLException s)
                return s;
        }
        return null;
    }

    private static String primeraLinea(String m) {
        if (m == null)
            return "Regla de negocio incumplida.";
        String limpio = m.startsWith("ERROR: ") ? m.substring(7) : m;
        int salto = limpio.indexOf('\n');
        return salto >= 0 ? limpio.substring(0, salto) : limpio;
    }

    private static String mensajeSeguro(int estado) {
        return switch (estado) {
            case 401 -> "Debe iniciar sesión.";
            case 403 -> "No tiene permiso para esta operación.";
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "Método no permitido.";
            case 415 -> "Tipo de contenido no soportado.";
            default -> "Petición inválida.";
        };
    }
}
