package ec.edu.praxis.application.oferta;

import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.application.excepcion.ReglaNegocioException;
import ec.edu.praxis.domain.modelo.*;
import ec.edu.praxis.domain.repositorio.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

@ApplicationScoped
public class GestionarOfertaPracticaService {

    private static final ZoneId ZONA_ECUADOR = ZoneId.of("America/Guayaquil");

    @Inject RepositorioOfertaPractica ofertas;
    @Inject RepositorioOfertaPerfil perfiles;
    @Inject RepositorioOfertaPerfilRequisito requisitos;
    @Inject RepositorioEmpresa empresas;
    @Inject RepositorioEmpresaUsuario miembros;
    @Inject RepositorioConvenio convenios;
    @Inject RepositorioTipoPractica tipos;
    @Inject RepositorioPeriodoAcademico periodos;
    @Inject EntityManager entityManager;

    public Map<String, Object> opciones(long actor) {
        List<EmpresaUsuario> asignaciones = miembros.listarTodas().stream()
                .filter(m -> Objects.equals(m.usuario_id, actor) && Boolean.TRUE.equals(m.activo))
                .toList();

        List<Map<String, Object>> empresasDisponibles = new ArrayList<>();
        List<Map<String, Object>> conveniosDisponibles = new ArrayList<>();
        LocalDate hoy = LocalDate.now(ZONA_ECUADOR);

        for (EmpresaUsuario miembro : asignaciones) {
            Empresa empresa = empresas.buscarPorId(miembro.empresa_id).orElse(null);
            if (empresa == null || !Boolean.TRUE.equals(empresa.activa)) continue;

            empresasDisponibles.add(Map.of(
                    "id", empresa.id,
                    "razon_social", empresa.razon_social,
                    "rol_empresa", miembro.rol_empresa
            ));

            for (Convenio convenio : convenios.listarTodos()) {
                if (Objects.equals(convenio.empresa_id, empresa.id)
                        && "VIGENTE".equals(convenio.estado)
                        && convenio.fecha_fin != null && !convenio.fecha_fin.isBefore(hoy)) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("id", convenio.id);
                    fila.put("empresa_id", convenio.empresa_id);
                    fila.put("numero", convenio.numero);
                    fila.put("fecha_inicio", convenio.fecha_inicio);
                    fila.put("fecha_fin", convenio.fecha_fin);
                    conveniosDisponibles.add(fila);
                }
            }
        }

        List<Map<String, Object>> tiposDisponibles = tipos.listarTodos().stream()
                .filter(t -> Boolean.TRUE.equals(t.activo))
                .map(t -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("id", t.id);
                    fila.put("nombre", t.nombre);
                    fila.put("horas_requeridas", t.horas_requeridas);
                    fila.put("nota_minima", t.nota_minima);
                    return fila;
                }).toList();

        List<Map<String, Object>> periodosDisponibles = periodos.listarTodos().stream()
                .filter(p -> Boolean.TRUE.equals(p.activo) && p.fecha_fin != null && !p.fecha_fin.isBefore(hoy))
                .map(p -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("id", p.id);
                    fila.put("codigo", p.codigo);
                    fila.put("nombre", p.nombre);
                    fila.put("fecha_inicio", p.fecha_inicio);
                    fila.put("fecha_fin", p.fecha_fin);
                    return fila;
                }).toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("empresas", empresasDisponibles);
        resultado.put("convenios", conveniosDisponibles);
        resultado.put("tiposPractica", tiposDisponibles);
        resultado.put("periodos", periodosDisponibles);
        return resultado;
    }

    public List<Map<String, Object>> listar(long actor) {
        Map<Long, EmpresaUsuario> membresias = new HashMap<>();
        miembros.listarTodas().stream()
                .filter(m -> Objects.equals(m.usuario_id, actor) && Boolean.TRUE.equals(m.activo))
                .forEach(m -> membresias.put(m.empresa_id, m));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (OfertaPractica oferta : ofertas.listarTodas()) {
            EmpresaUsuario miembro = membresias.get(oferta.empresa_id);
            if (miembro == null) continue;
            if ("PUBLICADOR".equals(miembro.rol_empresa)
                    && !Objects.equals(oferta.creada_por_usuario_id, actor)) continue;

            Empresa empresa = empresas.buscarPorId(oferta.empresa_id).orElse(null);
            Convenio convenio = convenios.buscarPorId(oferta.convenio_id).orElse(null);
            TipoPractica tipo = tipos.buscarPorId(oferta.tipo_practica_id).orElse(null);
            PeriodoAcademico periodo = periodos.buscarPorId(oferta.periodo_id).orElse(null);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", oferta.id);
            fila.put("empresa_id", oferta.empresa_id);
            fila.put("empresa", empresa == null ? "Empresa #" + oferta.empresa_id : empresa.razon_social);
            fila.put("titulo", oferta.titulo);
            fila.put("descripcion", oferta.descripcion);
            fila.put("fecha_inicio_actividades", oferta.fecha_inicio_actividades);
            fila.put("fecha_fin_actividades", oferta.fecha_fin_actividades);
            fila.put("convenio_id", oferta.convenio_id);
            fila.put("tipo_practica_id", oferta.tipo_practica_id);
            fila.put("periodo_id", oferta.periodo_id);
            fila.put("convenio", convenio == null ? "—" : convenio.numero);
            fila.put("tipo_practica", tipo == null ? "—" : tipo.nombre);
            fila.put("periodo", periodo == null ? "—" : periodo.nombre);
            fila.put("fecha_cierre", oferta.fecha_cierre);
            fila.put("estado", oferta.estado);
            fila.put("motivo_decision", oferta.motivo_decision);
            fila.put("creada_por_usuario_id", oferta.creada_por_usuario_id);
            fila.put("rol_empresa", miembro.rol_empresa);
            fila.put("perfiles", perfiles.listarPorOferta(oferta.id).stream().map(perfil -> {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id", perfil.id);
                p.put("nombre", perfil.nombre);
                p.put("descripcion", perfil.descripcion);
                p.put("plazas", perfil.plazas);
                p.put("requisitos", requisitos.listarPorPerfil(perfil.id).stream().map(req -> req.descripcion).toList());
                return p;
            }).toList());
            resultado.add(fila);
        }
        return resultado;
    }

    @Transactional
    public long crear(Map<String, Object> datos, long actor) {
        establecerActor(actor);

        long empresaId = numero(datos, "empresaId");
        EmpresaUsuario miembro = membresiasActivas(actor).stream()
                .filter(m -> Objects.equals(m.empresa_id, empresaId))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("No tienes acceso activo a la empresa seleccionada."));
        Empresa empresa = empresas.buscarPorId(empresaId)
                .orElseThrow(() -> new NoEncontradoException("Empresa no encontrada."));
        if (!Boolean.TRUE.equals(empresa.activa)) {
            throw new ReglaNegocioException("La empresa está inactiva.");
        }

        long convenioId = numero(datos, "convenioId");
        Convenio convenio = convenios.buscarPorId(convenioId)
                .orElseThrow(() -> new NoEncontradoException("Convenio no encontrado."));
        if (!Objects.equals(convenio.empresa_id, empresaId)) {
            throw new ReglaNegocioException("El convenio seleccionado no pertenece a la empresa.");
        }

        long tipoId = numero(datos, "tipoPracticaId");
        TipoPractica tipo = tipos.buscarPorId(tipoId)
                .orElseThrow(() -> new NoEncontradoException("Tipo de práctica no encontrado."));
        if (!Boolean.TRUE.equals(tipo.activo)) throw new ReglaNegocioException("El tipo de práctica está inactivo.");

        long periodoId = numero(datos, "periodoId");
        PeriodoAcademico periodo = periodos.buscarPorId(periodoId)
                .orElseThrow(() -> new NoEncontradoException("Periodo académico no encontrado."));
        if (!Boolean.TRUE.equals(periodo.activo)) throw new ReglaNegocioException("El periodo académico no está activo.");

        String titulo = requerido(datos, "titulo");
        if (titulo.length() > 200) throw new IllegalArgumentException("El título no puede superar 200 caracteres.");
        LocalDate inicio = fecha(datos, "fechaInicioActividades");
        LocalDate fin = fecha(datos, "fechaFinActividades");
        LocalDate cierre = fecha(datos, "fechaCierre");
        if (fin.isBefore(inicio)) throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        if (cierre.isAfter(inicio)) throw new IllegalArgumentException("La fecha de cierre debe ser anterior o igual al inicio de actividades.");

        String nombrePerfil = requerido(datos, "perfilNombre");
        if (nombrePerfil.length() > 150) throw new IllegalArgumentException("El nombre del perfil no puede superar 150 caracteres.");
        int plazas = (int) numero(datos, "plazas");
        if (plazas < 1) throw new IllegalArgumentException("El número de plazas debe ser mayor que cero.");

        OffsetDateTime ahora = OffsetDateTime.now();
        OfertaPractica oferta = new OfertaPractica();
        oferta.empresa_id = empresaId;
        oferta.convenio_id = convenioId;
        oferta.tipo_practica_id = tipoId;
        oferta.periodo_id = periodoId;
        oferta.titulo = titulo;
        oferta.descripcion = opcional(datos, "descripcion");
        oferta.fecha_inicio_actividades = inicio;
        oferta.fecha_fin_actividades = fin;
        oferta.fecha_cierre = cierre;
        oferta.estado = "BORRADOR";
        oferta.creada_por_usuario_id = actor;
        oferta.created_at = ahora;
        oferta.updated_at = ahora;
        ofertas.guardar(oferta);
        entityManager.flush();

        OfertaPerfil perfil = new OfertaPerfil();
        perfil.oferta_id = oferta.id;
        perfil.nombre = nombrePerfil;
        perfil.descripcion = opcional(datos, "perfilDescripcion");
        perfil.plazas = plazas;
        perfil.created_at = ahora;
        perfil.updated_at = ahora;
        perfiles.guardar(perfil);
        entityManager.flush();

        Object lista = datos.get("requisitos");
        if (lista instanceof List<?> elementos) {
            for (Object elemento : elementos) {
                String descripcion = elemento == null ? "" : elemento.toString().trim();
                if (descripcion.isEmpty()) continue;
                if (descripcion.length() > 2000) throw new IllegalArgumentException("Un requisito supera los 2000 caracteres.");
                OfertaPerfilRequisito requisito = new OfertaPerfilRequisito();
                requisito.perfil_id = perfil.id;
                requisito.descripcion = descripcion;
                requisito.obligatorio = true;
                requisito.created_at = ahora;
                requisito.updated_at = ahora;
                requisitos.guardar(requisito);
            }
        }

        return oferta.id;
    }

    @Transactional
    public void actualizar(long ofertaId, Map<String, Object> datos, long actor) {
        establecerActor(actor);
        OfertaPractica oferta = ofertas.buscarPorId(ofertaId)
                .orElseThrow(() -> new NoEncontradoException("Oferta no encontrada."));
        if (!"BORRADOR".equals(oferta.estado) && !"RECHAZADA".equals(oferta.estado)) {
            throw new ReglaNegocioException("Solo se pueden editar ofertas en estado BORRADOR o RECHAZADA.");
        }
        EmpresaUsuario miembro = membresiasActivas(actor).stream()
                .filter(m -> Objects.equals(m.empresa_id, oferta.empresa_id))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("No tienes acceso activo a esta empresa."));
        if ("PUBLICADOR".equals(miembro.rol_empresa)
                && !Objects.equals(oferta.creada_por_usuario_id, actor)) {
            throw new ReglaNegocioException("Un publicador solo puede editar sus propias ofertas.");
        }

        long convenioId = numero(datos, "convenioId");
        Convenio convenio = convenios.buscarPorId(convenioId)
                .orElseThrow(() -> new NoEncontradoException("Convenio no encontrado."));
        if (!Objects.equals(convenio.empresa_id, oferta.empresa_id)) {
            throw new ReglaNegocioException("El convenio seleccionado no pertenece a la empresa de la oferta.");
        }
        long tipoId = numero(datos, "tipoPracticaId");
        TipoPractica tipo = tipos.buscarPorId(tipoId)
                .orElseThrow(() -> new NoEncontradoException("Tipo de práctica no encontrado."));
        if (!Boolean.TRUE.equals(tipo.activo)) throw new ReglaNegocioException("El tipo de práctica está inactivo.");
        long periodoId = numero(datos, "periodoId");
        PeriodoAcademico periodo = periodos.buscarPorId(periodoId)
                .orElseThrow(() -> new NoEncontradoException("Periodo académico no encontrado."));
        if (!Boolean.TRUE.equals(periodo.activo)) throw new ReglaNegocioException("El periodo académico no está activo.");

        String titulo = requerido(datos, "titulo");
        if (titulo.length() > 200) throw new IllegalArgumentException("El título no puede superar 200 caracteres.");
        LocalDate inicio = fecha(datos, "fechaInicioActividades");
        LocalDate fin = fecha(datos, "fechaFinActividades");
        LocalDate cierre = fecha(datos, "fechaCierre");
        if (fin.isBefore(inicio)) throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        if (cierre.isAfter(inicio)) throw new IllegalArgumentException("La fecha de cierre debe ser anterior o igual al inicio de actividades.");

        oferta.convenio_id = convenioId;
        oferta.tipo_practica_id = tipoId;
        oferta.periodo_id = periodoId;
        oferta.titulo = titulo;
        oferta.descripcion = opcional(datos, "descripcion");
        oferta.fecha_inicio_actividades = inicio;
        oferta.fecha_fin_actividades = fin;
        oferta.fecha_cierre = cierre;
        oferta.updated_at = OffsetDateTime.now();
        ofertas.guardar(oferta);

        List<OfertaPerfil> perfilesOferta = perfiles.listarPorOferta(oferta.id);
        if (perfilesOferta.isEmpty()) {
            throw new ReglaNegocioException("La oferta no tiene un perfil que pueda actualizarse.");
        }
        OfertaPerfil perfil = perfilesOferta.get(0);
        String nombrePerfil = requerido(datos, "perfilNombre");
        if (nombrePerfil.length() > 150) throw new IllegalArgumentException("El nombre del perfil no puede superar 150 caracteres.");
        int plazas = (int) numero(datos, "plazas");
        if (plazas < 1) throw new IllegalArgumentException("El número de plazas debe ser mayor que cero.");
        perfil.nombre = nombrePerfil;
        perfil.descripcion = opcional(datos, "perfilDescripcion");
        perfil.plazas = plazas;
        perfil.updated_at = OffsetDateTime.now();
        perfiles.guardar(perfil);

        for (OfertaPerfilRequisito requisito : requisitos.listarPorPerfil(perfil.id)) {
            requisitos.eliminar(requisito);
        }
        Object lista = datos.get("requisitos");
        if (lista instanceof List<?> elementos) {
            for (Object elemento : elementos) {
                String descripcion = elemento == null ? "" : elemento.toString().trim();
                if (descripcion.isEmpty()) continue;
                if (descripcion.length() > 2000) throw new IllegalArgumentException("Un requisito supera los 2000 caracteres.");
                OfertaPerfilRequisito requisito = new OfertaPerfilRequisito();
                requisito.perfil_id = perfil.id;
                requisito.descripcion = descripcion;
                requisito.obligatorio = true;
                requisito.created_at = OffsetDateTime.now();
                requisito.updated_at = requisito.created_at;
                requisitos.guardar(requisito);
            }
        }
        entityManager.flush();
    }

    @Transactional
    public void enviarRevision(long ofertaId, long actor) {
        establecerActor(actor);
        OfertaPractica oferta = ofertas.buscarPorId(ofertaId)
                .orElseThrow(() -> new NoEncontradoException("Oferta no encontrada."));
        EmpresaUsuario miembro = membresiasActivas(actor).stream()
                .filter(m -> Objects.equals(m.empresa_id, oferta.empresa_id))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("No tienes acceso activo a esta empresa."));

        if ("PUBLICADOR".equals(miembro.rol_empresa)
                && !Objects.equals(oferta.creada_por_usuario_id, actor)) {
            throw new ReglaNegocioException("Un publicador solo puede enviar sus propias ofertas.");
        }
        if (!"BORRADOR".equals(oferta.estado) && !"RECHAZADA".equals(oferta.estado)) {
            throw new ReglaNegocioException("Solo se pueden enviar ofertas en estado BORRADOR o RECHAZADA.");
        }
        if (perfiles.listarPorOferta(oferta.id).isEmpty()) {
            throw new ReglaNegocioException("La oferta debe tener al menos un perfil con plazas.");
        }

        oferta.estado = "PENDIENTE_REVISION";
        oferta.motivo_decision = null;
        oferta.updated_at = OffsetDateTime.now();
        ofertas.guardar(oferta);
        entityManager.flush();
    }

    private List<EmpresaUsuario> membresiasActivas(long actor) {
        return miembros.listarTodas().stream()
                .filter(m -> Objects.equals(m.usuario_id, actor) && Boolean.TRUE.equals(m.activo))
                .toList();
    }

    private void establecerActor(long actor) {
        entityManager.createNativeQuery("SELECT set_config('praxis.usuario_id', ?1, true)")
                .setParameter(1, Long.toString(actor))
                .getSingleResult();
    }

    private long numero(Map<String, Object> datos, String campo) {
        Object valor = datos == null ? null : datos.get(campo);
        if (!(valor instanceof Number)) {
            try {
                return Long.parseLong(Objects.toString(valor, ""));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Seleccione un valor válido para " + campo + ".");
            }
        }
        return ((Number) valor).longValue();
    }

    private String requerido(Map<String, Object> datos, String campo) {
        String valor = opcional(datos, campo);
        if (valor == null) throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        return valor;
    }

    private String opcional(Map<String, Object> datos, String campo) {
        Object valor = datos == null ? null : datos.get(campo);
        if (valor == null) return null;
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private LocalDate fecha(Map<String, Object> datos, String campo) {
        try {
            return LocalDate.parse(requerido(datos, campo));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha " + campo + " debe tener formato AAAA-MM-DD.");
        }
    }
}
