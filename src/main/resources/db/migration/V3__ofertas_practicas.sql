-- ============================================================
-- PRAXIS — V3__ofertas_practicas.sql  (PostgreSQL 15+)
-- Convocatorias empresariales con perfiles y plazas.
--
-- Principio: la empresa propone y recomienda; la UNIVERSIDAD (coordinador)
-- aprueba las ofertas y decide la aceptación de cada postulación.
-- Los triggers son una SEGUNDA barrera: la API debe autorizar igualmente.
--
-- No modifica V1 ni V2 como archivos. Cambios sobre objetos existentes:
--   * rol.chk_rol_codigo se amplía con REPRESENTANTE_EMPRESA
--   * practica.postulacion_id (nullable) enlaza la práctica con su postulación
--   * historial inmutable de cambios en perfiles, carreras y requisitos de ofertas
--
-- Contexto que el backend debe fijar en CADA transacción (set_config(..., true)):
--   praxis.usuario_id  -> usuario autenticado que ejecuta la operación
--   praxis.motivo      -> motivo de la operación (rechazos, cancelaciones, correcciones)
--   praxis.sistema     -> 'true' SOLO en procesos automáticos sin usuario (cierre de ofertas)
--   praxis.desvincular_postulacion -> 'true' solo en la operación administrativa explícita
--                                     que desvincula una práctica de su postulación
--   praxis.correccion_decision     -> 'true' solo en la operación administrativa (coordinador
--                                     o administrador, con praxis.motivo) que corrige una
--                                     postulación ya resuelta; queda en auditoria
--
-- Orden uniforme que DEBE respetar la API antes de ejecutar UPDATE sobre una postulación:
--   oferta_practica -> oferta_perfil -> postulacion
-- PostgreSQL bloquea la fila destino de postulacion antes de ejecutar su trigger BEFORE;
-- por eso el servicio debe bloquear oferta/perfil previamente para respetar este orden.
-- ============================================================

-- ============================================================
-- 0. ROL PARA USUARIOS DE EMPRESA QUE PUBLICAN OFERTAS
-- ============================================================

ALTER TABLE rol DROP CONSTRAINT chk_rol_codigo;
ALTER TABLE rol ADD CONSTRAINT chk_rol_codigo CHECK (codigo IN (
    'ESTUDIANTE','DOCENTE','TUTOR_EMPRESARIAL',
    'COORDINADOR','ADMINISTRADOR','AUTORIDAD','REPRESENTANTE_EMPRESA'));

INSERT INTO rol (codigo, nombre, descripcion) VALUES
('REPRESENTANTE_EMPRESA', 'Representante de empresa',
 'Publica y administra convocatorias de su empresa; recomienda postulantes')
ON CONFLICT (codigo) DO NOTHING;

-- ============================================================
-- 1. FUNCIONES AUXILIARES DE IDENTIDAD Y PERMISOS
-- ============================================================

CREATE OR REPLACE FUNCTION praxis_actor()
RETURNS BIGINT AS $$
    SELECT NULLIF(current_setting('praxis.usuario_id', TRUE), '')::BIGINT
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION praxis_sistema()
RETURNS BOOLEAN AS $$
    SELECT COALESCE(current_setting('praxis.sistema', TRUE), '') = 'true'
$$ LANGUAGE sql STABLE;

-- usuario activo con un rol global concreto
CREATE OR REPLACE FUNCTION tiene_rol_activo(p_usuario BIGINT, p_codigo TEXT)
RETURNS BOOLEAN AS $$
    SELECT COALESCE((
        SELECT TRUE
          FROM usuario u
          JOIN usuario_rol ur ON ur.usuario_id = u.id
          JOIN rol r          ON r.id = ur.rol_id
         WHERE u.id = p_usuario AND u.activo AND r.codigo = p_codigo
         LIMIT 1), FALSE)
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION es_coordinador_activo(p_usuario BIGINT)
RETURNS BOOLEAN AS $$
    SELECT tiene_rol_activo(p_usuario, 'COORDINADOR')
$$ LANGUAGE sql STABLE;

-- ============================================================
-- 2. USUARIOS DE EMPRESA
-- ============================================================

CREATE TABLE empresa_usuario (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id  BIGINT      NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT,
    usuario_id  BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    rol_empresa VARCHAR(20) NOT NULL DEFAULT 'PUBLICADOR',
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_empresa_usuario UNIQUE (empresa_id, usuario_id),
    CONSTRAINT chk_empresa_usuario_rol CHECK (rol_empresa IN ('ADMIN_EMPRESA','PUBLICADOR'))
);
CREATE INDEX idx_empresa_usuario_usuario ON empresa_usuario(usuario_id);

-- membresía EFECTIVA: usuario activo + rol global REPRESENTANTE_EMPRESA + membresía activa.
-- Se evalúa en cada operación, así un usuario que pierde el rol o se desactiva pierde permisos
-- aunque su fila de empresa_usuario siga marcada como activa.
CREATE OR REPLACE FUNCTION empresa_membresia_id(p_usuario BIGINT, p_empresa BIGINT)
RETURNS BIGINT AS $$
    SELECT eu.id
      FROM empresa_usuario eu
      JOIN usuario u ON u.id = eu.usuario_id AND u.activo
     WHERE eu.usuario_id = p_usuario AND eu.empresa_id = p_empresa AND eu.activo
       AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id = ur.rol_id
                    WHERE ur.usuario_id = eu.usuario_id
                      AND r.codigo = 'REPRESENTANTE_EMPRESA')
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION empresa_rol_activo(p_usuario BIGINT, p_empresa BIGINT)
RETURNS VARCHAR AS $$
    SELECT eu.rol_empresa
      FROM empresa_usuario eu
     WHERE eu.id = empresa_membresia_id(p_usuario, p_empresa)
$$ LANGUAGE sql STABLE;

-- ADMIN_EMPRESA gestiona cualquier oferta de su empresa; PUBLICADOR solo las que creó
CREATE OR REPLACE FUNCTION puede_gestionar_oferta(p_actor BIGINT, p_empresa BIGINT, p_creador BIGINT)
RETURNS BOOLEAN AS $$
    SELECT COALESCE(
        CASE empresa_rol_activo(p_actor, p_empresa)
            WHEN 'ADMIN_EMPRESA' THEN TRUE
            WHEN 'PUBLICADOR'    THEN p_actor = p_creador
            ELSE FALSE
        END, FALSE)
$$ LANGUAGE sql STABLE;

-- ============================================================
-- 3. OFERTA (convocatoria), HISTORIAL, PERFILES Y REQUISITOS
-- ============================================================

CREATE TABLE oferta_practica (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id               BIGINT       NOT NULL REFERENCES empresa(id)           ON DELETE RESTRICT,
    convenio_id              BIGINT       NOT NULL,
    tipo_practica_id         BIGINT       NOT NULL REFERENCES tipo_practica(id)     ON DELETE RESTRICT,
    periodo_id               BIGINT       NOT NULL REFERENCES periodo_academico(id) ON DELETE RESTRICT,
    titulo                   VARCHAR(200) NOT NULL,
    descripcion              TEXT,
    fecha_inicio_actividades DATE         NOT NULL,
    fecha_fin_actividades    DATE         NOT NULL,
    fecha_cierre             DATE         NOT NULL,   -- último día para postular
    horas_requeridas_snapshot NUMERIC(6,2),             -- congeladas al publicar la oferta
    nota_minima_snapshot      NUMERIC(4,2),
    fecha_primera_publicacion TIMESTAMPTZ,            -- primera aprobación histórica
    fecha_publicacion        TIMESTAMPTZ,             -- última publicación / republicación
    estado                   VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    creada_por_usuario_id    BIGINT       NOT NULL,
    resuelta_por_usuario_id  BIGINT       REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_resolucion         TIMESTAMPTZ,
    motivo_decision          TEXT,                    -- motivo de la ÚLTIMA transición
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_oferta_convenio_empresa
        FOREIGN KEY (convenio_id, empresa_id)
        REFERENCES convenio(id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_oferta_creador_empresa
        FOREIGN KEY (empresa_id, creada_por_usuario_id)
        REFERENCES empresa_usuario(empresa_id, usuario_id) ON DELETE RESTRICT,

    CONSTRAINT chk_oferta_estado CHECK (estado IN (
        'BORRADOR','PENDIENTE_REVISION','PUBLICADA','RECHAZADA','CERRADA','CANCELADA')),
    CONSTRAINT chk_oferta_titulo CHECK (length(btrim(titulo)) > 0),
    CONSTRAINT chk_oferta_fechas_actividad CHECK (fecha_fin_actividades >= fecha_inicio_actividades),
    CONSTRAINT chk_oferta_cierre_antes_inicio CHECK (fecha_cierre <= fecha_inicio_actividades),
    CONSTRAINT chk_oferta_snapshot CHECK (
        (horas_requeridas_snapshot IS NULL AND nota_minima_snapshot IS NULL
            AND estado NOT IN ('PUBLICADA','CERRADA'))
        OR (horas_requeridas_snapshot IS NOT NULL
            AND nota_minima_snapshot IS NOT NULL
            AND horas_requeridas_snapshot > 0
            AND nota_minima_snapshot BETWEEN 0 AND 20)),
    CONSTRAINT chk_oferta_resolucion CHECK (
        (estado IN ('PUBLICADA','RECHAZADA')
            AND resuelta_por_usuario_id IS NOT NULL AND fecha_resolucion IS NOT NULL)
        OR (estado = 'CERRADA' AND fecha_resolucion IS NOT NULL)
        OR estado NOT IN ('PUBLICADA','RECHAZADA','CERRADA')),
    CONSTRAINT chk_oferta_publicacion CHECK (
        estado NOT IN ('PUBLICADA','CERRADA')
        OR (fecha_publicacion IS NOT NULL AND fecha_primera_publicacion IS NOT NULL)),
    CONSTRAINT chk_oferta_motivo CHECK (
        estado NOT IN ('RECHAZADA','CANCELADA')
        OR (motivo_decision IS NOT NULL AND length(btrim(motivo_decision)) > 0))
);
CREATE INDEX idx_oferta_empresa_estado ON oferta_practica(empresa_id, estado);
CREATE INDEX idx_oferta_estado_cierre  ON oferta_practica(estado, fecha_cierre);
CREATE INDEX idx_oferta_convenio       ON oferta_practica(convenio_id, empresa_id);
CREATE INDEX idx_oferta_periodo        ON oferta_practica(periodo_id);
CREATE INDEX idx_oferta_tipo           ON oferta_practica(tipo_practica_id);
CREATE INDEX idx_oferta_creador        ON oferta_practica(creada_por_usuario_id);

CREATE TABLE oferta_estado_historial (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oferta_id       BIGINT      NOT NULL REFERENCES oferta_practica(id) ON DELETE RESTRICT,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20) NOT NULL,
    usuario_id      BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    es_sistema      BOOLEAN     NOT NULL DEFAULT FALSE,
    motivo          TEXT,
    fecha           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ofhist_anterior CHECK (
        estado_anterior IS NULL OR estado_anterior IN (
            'BORRADOR','PENDIENTE_REVISION','PUBLICADA','RECHAZADA','CERRADA','CANCELADA')),
    CONSTRAINT chk_ofhist_nuevo CHECK (estado_nuevo IN (
        'BORRADOR','PENDIENTE_REVISION','PUBLICADA','RECHAZADA','CERRADA','CANCELADA')),
    CONSTRAINT chk_ofhist_responsable CHECK (usuario_id IS NOT NULL OR es_sistema),
    CONSTRAINT chk_ofhist_motivo CHECK (
        estado_nuevo NOT IN ('RECHAZADA','CANCELADA')
        OR (motivo IS NOT NULL AND length(btrim(motivo)) > 0))
);
CREATE INDEX idx_ofhist_oferta_fecha ON oferta_estado_historial(oferta_id, fecha DESC);

CREATE TABLE oferta_perfil (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oferta_id   BIGINT       NOT NULL REFERENCES oferta_practica(id) ON DELETE RESTRICT,
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT,
    plazas      INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_perfil_oferta_nombre UNIQUE (oferta_id, nombre),
    CONSTRAINT chk_perfil_plazas CHECK (plazas > 0)
);
CREATE INDEX idx_perfil_oferta ON oferta_perfil(oferta_id);

-- Carreras a las que va dirigido el perfil (sin filas = abierto a todas)
CREATE TABLE oferta_perfil_carrera (
    perfil_id  BIGINT      NOT NULL REFERENCES oferta_perfil(id) ON DELETE CASCADE,
    carrera_id BIGINT      NOT NULL REFERENCES carrera(id)       ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (perfil_id, carrera_id)
);
CREATE INDEX idx_perfil_carrera_carrera ON oferta_perfil_carrera(carrera_id);

-- Requisitos laborales del perfil (independientes de la tabla requisito académica)
CREATE TABLE oferta_perfil_requisito (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    perfil_id   BIGINT      NOT NULL REFERENCES oferta_perfil(id) ON DELETE CASCADE,
    descripcion TEXT        NOT NULL,
    obligatorio BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_perfil_req_desc CHECK (length(btrim(descripcion)) > 0)
);
CREATE INDEX idx_perfil_req_perfil ON oferta_perfil_requisito(perfil_id);

-- ============================================================
-- 4. POSTULACIONES
--    Etapa empresarial (recomendación) separada de la decisión académica (estado).
-- ============================================================

CREATE TABLE postulacion (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    perfil_id         BIGINT      NOT NULL REFERENCES oferta_perfil(id) ON DELETE RESTRICT,
    estudiante_id     BIGINT      NOT NULL REFERENCES estudiante(id)    ON DELETE RESTRICT,
    estado            VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    mensaje           TEXT,
    fecha_postulacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- recomendación vigente de la empresa (el historial completo está aparte)
    recomendacion_empresa           VARCHAR(20) NOT NULL DEFAULT 'SIN_EVALUAR',
    observacion_empresa             TEXT,
    evaluada_por_empresa_usuario_id BIGINT REFERENCES empresa_usuario(id) ON DELETE RESTRICT,
    fecha_evaluacion_empresa        TIMESTAMPTZ,

    -- decisión académica (solo coordinador)
    resuelta_por      BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_resolucion  TIMESTAMPTZ,
    motivo_decision   TEXT,

    -- cancelación de una aceptada
    cancelada_por     BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_cancelacion TIMESTAMPTZ,

    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_postulacion_estado CHECK (estado IN (
        'PENDIENTE','ACEPTADA','RECHAZADA','RETIRADA','CANCELADA')),
    CONSTRAINT chk_postulacion_recomendacion CHECK (
        recomendacion_empresa IN ('SIN_EVALUAR','RECOMENDADO','NO_RECOMENDADO')),
    CONSTRAINT chk_postulacion_eval_empresa CHECK (
        (recomendacion_empresa = 'SIN_EVALUAR'
            AND observacion_empresa IS NULL
            AND evaluada_por_empresa_usuario_id IS NULL
            AND fecha_evaluacion_empresa IS NULL)
        OR (recomendacion_empresa <> 'SIN_EVALUAR'
            AND evaluada_por_empresa_usuario_id IS NOT NULL
            AND fecha_evaluacion_empresa IS NOT NULL)),
    CONSTRAINT chk_postulacion_resolucion CHECK (
        (estado IN ('PENDIENTE','RETIRADA') AND resuelta_por IS NULL AND fecha_resolucion IS NULL)
        OR estado = 'CANCELADA'
        OR (estado IN ('ACEPTADA','RECHAZADA')
            AND resuelta_por IS NOT NULL AND fecha_resolucion IS NOT NULL)),
    CONSTRAINT chk_postulacion_cancelacion CHECK (
        (estado = 'CANCELADA' AND cancelada_por IS NOT NULL AND fecha_cancelacion IS NOT NULL)
        OR (estado <> 'CANCELADA' AND cancelada_por IS NULL AND fecha_cancelacion IS NULL)),
    CONSTRAINT chk_postulacion_motivo CHECK (
        estado NOT IN ('RECHAZADA','CANCELADA')
        OR (motivo_decision IS NOT NULL AND length(btrim(motivo_decision)) > 0))
);
-- Un estudiante no repite perfil: rechazado/aceptado/cancelado bloquean; retirada permite volver a postular
CREATE UNIQUE INDEX uq_postulacion_perfil_estudiante
    ON postulacion(perfil_id, estudiante_id) WHERE estado <> 'RETIRADA';
CREATE INDEX idx_postulacion_perfil_estado ON postulacion(perfil_id, estado);
CREATE INDEX idx_postulacion_estudiante    ON postulacion(estudiante_id, estado);

-- Historial de recomendaciones empresariales (inmutable)
CREATE TABLE postulacion_recomendacion_historial (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    postulacion_id      BIGINT      NOT NULL REFERENCES postulacion(id)     ON DELETE RESTRICT,
    recomendacion       VARCHAR(20) NOT NULL,
    observacion         TEXT,
    empresa_usuario_id  BIGINT      NOT NULL REFERENCES empresa_usuario(id) ON DELETE RESTRICT,
    usuario_id          BIGINT      NOT NULL REFERENCES usuario(id)         ON DELETE RESTRICT,
    fecha               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_posthist_recomendacion CHECK (recomendacion IN ('RECOMENDADO','NO_RECOMENDADO'))
);
CREATE INDEX idx_posthist_postulacion ON postulacion_recomendacion_historial(postulacion_id, fecha DESC);

-- ============================================================
-- 5. ENLACE practica -> postulacion
-- ============================================================

ALTER TABLE practica
    ADD COLUMN postulacion_id BIGINT UNIQUE
        REFERENCES postulacion(id) ON DELETE RESTRICT;   -- NULL = práctica directa, sin convocatoria

-- ============================================================
-- 6. VISTA DE CUPOS (el cierre del perfil se deriva, no se almacena)
-- ============================================================

CREATE OR REPLACE VIEW v_oferta_perfil_cupo AS
SELECT p.id AS perfil_id, p.oferta_id, p.nombre, p.plazas,
       COUNT(po.id) FILTER (WHERE po.estado = 'ACEPTADA')  AS aceptadas,
       COUNT(po.id) FILTER (WHERE po.estado = 'PENDIENTE') AS pendientes,
       COUNT(po.id) FILTER (WHERE po.estado = 'RECHAZADA') AS rechazadas,
       COUNT(po.id) FILTER (WHERE po.estado = 'RETIRADA')  AS retiradas,
       p.plazas - COUNT(po.id) FILTER (WHERE po.estado = 'ACEPTADA') AS disponibles,
       (COUNT(po.id) FILTER (WHERE po.estado = 'ACEPTADA') >= p.plazas) AS perfil_cerrado
  FROM oferta_perfil p
  LEFT JOIN postulacion po ON po.perfil_id = p.id
 GROUP BY p.id, p.oferta_id, p.nombre, p.plazas;

-- ============================================================
-- 7. TRIGGERS
-- ============================================================

-- 7.1 updated_at
CREATE TRIGGER trg_empresa_usuario_updated BEFORE UPDATE ON empresa_usuario
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_oferta_practica_updated BEFORE UPDATE ON oferta_practica
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_oferta_perfil_updated BEFORE UPDATE ON oferta_perfil
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_oferta_perfil_requisito_updated BEFORE UPDATE ON oferta_perfil_requisito
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_postulacion_updated BEFORE UPDATE ON postulacion
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 7.2 empresa_usuario
--     * solo coordinador/administrador o ADMIN_EMPRESA de esa empresa administra miembros
--     * toda membresía activa exige usuario activo y rol REPRESENTANTE_EMPRESA
--     * el primer miembro activo de una empresa debe ser ADMIN_EMPRESA
--     * la empresa nunca queda sin ADMIN_EMPRESA activo
CREATE OR REPLACE FUNCTION validar_empresa_usuario()
RETURNS TRIGGER AS $$
DECLARE
    v_actor          BIGINT := praxis_actor();
    v_empresa_activa BOOLEAN;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'empresa_usuario no se borra; desactive el acceso (activo = false)';
    END IF;

    -- serializa cambios de miembros de una misma empresa
    PERFORM 1 FROM empresa WHERE id = NEW.empresa_id FOR UPDATE;
    SELECT activa INTO v_empresa_activa FROM empresa WHERE id = NEW.empresa_id;

    IF v_actor IS NULL THEN
        RAISE EXCEPTION 'Falta praxis.usuario_id: no se identifica al responsable del cambio';
    END IF;
    IF NOT (tiene_rol_activo(v_actor, 'COORDINADOR')
            OR tiene_rol_activo(v_actor, 'ADMINISTRADOR')
            OR COALESCE(empresa_rol_activo(v_actor, NEW.empresa_id), '') = 'ADMIN_EMPRESA') THEN
        RAISE EXCEPTION
            'Solo un coordinador, un administrador o el ADMIN_EMPRESA de la empresa gestiona sus usuarios';
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF (OLD.empresa_id, OLD.usuario_id) IS DISTINCT FROM (NEW.empresa_id, NEW.usuario_id) THEN
            RAISE EXCEPTION 'empresa_id y usuario_id de empresa_usuario son inmutables';
        END IF;
    END IF;

    IF NEW.activo THEN
        IF NOT v_empresa_activa THEN
            RAISE EXCEPTION 'No se puede mantener usuarios activos en una empresa inactiva';
        END IF;
        IF NOT tiene_rol_activo(NEW.usuario_id, 'REPRESENTANTE_EMPRESA') THEN
            RAISE EXCEPTION 'El usuario debe estar activo y tener el rol REPRESENTANTE_EMPRESA';
        END IF;
        -- primer miembro activo = administrador
        IF NEW.rol_empresa <> 'ADMIN_EMPRESA'
           AND NOT EXISTS (SELECT 1 FROM empresa_usuario
                            WHERE empresa_id = NEW.empresa_id AND id IS DISTINCT FROM NEW.id
                              AND activo) THEN
            RAISE EXCEPTION 'El primer usuario activo de una empresa debe ser ADMIN_EMPRESA';
        END IF;
    END IF;

    IF TG_OP = 'UPDATE'
       AND OLD.activo AND OLD.rol_empresa = 'ADMIN_EMPRESA'
       AND (NOT NEW.activo OR NEW.rol_empresa <> 'ADMIN_EMPRESA')
       AND v_empresa_activa THEN
        IF NOT EXISTS (SELECT 1 FROM empresa_usuario
                        WHERE empresa_id = NEW.empresa_id AND id <> NEW.id
                          AND activo AND rol_empresa = 'ADMIN_EMPRESA') THEN
            RAISE EXCEPTION 'La empresa no puede quedar sin un ADMIN_EMPRESA activo';
        END IF;
    END IF;

    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_empresa_usuario
    BEFORE INSERT OR UPDATE OR DELETE ON empresa_usuario
    FOR EACH ROW EXECUTE FUNCTION validar_empresa_usuario();

CREATE OR REPLACE FUNCTION auditar_empresa_usuario()
RETURNS TRIGGER AS $$
BEGIN
    IF praxis_actor() IS NULL THEN
        RAISE EXCEPTION 'Falta praxis.usuario_id para auditar la membresía empresarial';
    END IF;
    INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle)
    VALUES (praxis_actor(), 'EMPRESA_USUARIO_' || TG_OP, 'empresa_usuario',
            CASE WHEN TG_OP = 'DELETE' THEN OLD.id ELSE NEW.id END,
            jsonb_build_object(
                'antes', CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN to_jsonb(OLD) END,
                'despues', CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN to_jsonb(NEW) END));
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditar_empresa_usuario
    AFTER INSERT OR UPDATE ON empresa_usuario
    FOR EACH ROW EXECUTE FUNCTION auditar_empresa_usuario();

-- 7.3 oferta_practica: autorización por acción, cambios sustanciales,
--     estados y revalidaciones
CREATE OR REPLACE FUNCTION validar_oferta()
RETURNS TRIGGER AS $$
DECLARE
    v_hoy            DATE := (NOW() AT TIME ZONE 'America/Guayaquil')::date;
    v_actor          BIGINT := praxis_actor();
    v_empresa_activa BOOLEAN;
    v_tipo_activo    BOOLEAN;
    v_per_ini        DATE;
    v_per_fin        DATE;
    v_periodo_activo BOOLEAN;
    v_conv_estado    TEXT;
    v_conv_inicio    DATE;
    v_conv_fin       DATE;
    v_tipo_horas     NUMERIC(6,2);
    v_tipo_nota      NUMERIC(4,2);
    v_sustancial     BOOLEAN;
    v_motivo_nuevo   BOOLEAN;
    v_auto           BOOLEAN := FALSE;
    v_revalidar      BOOLEAN;
BEGIN
    -- ---------- INSERT ----------
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'BORRADOR' THEN
            RAISE EXCEPTION 'Una oferta nueva debe iniciar en BORRADOR';
        END IF;
        IF NEW.horas_requeridas_snapshot IS NOT NULL OR NEW.nota_minima_snapshot IS NOT NULL THEN
            RAISE EXCEPTION 'Los snapshots académicos se generan al enviar la oferta a revisión';
        END IF;
        IF v_actor IS DISTINCT FROM NEW.creada_por_usuario_id THEN
            RAISE EXCEPTION 'La oferta debe crearla el usuario autenticado (praxis.usuario_id)';
        END IF;
        IF empresa_rol_activo(v_actor, NEW.empresa_id) IS NULL THEN
            RAISE EXCEPTION 'El usuario no es un miembro activo y autorizado de la empresa';
        END IF;
        SELECT activa INTO v_empresa_activa FROM empresa WHERE id = NEW.empresa_id;
        IF NOT v_empresa_activa THEN
            RAISE EXCEPTION 'La empresa está inactiva y no puede crear ofertas';
        END IF;
        RETURN NEW;
    END IF;

    -- ---------- UPDATE ----------
    IF (OLD.empresa_id, OLD.creada_por_usuario_id)
       IS DISTINCT FROM (NEW.empresa_id, NEW.creada_por_usuario_id) THEN
        RAISE EXCEPTION 'empresa_id y creada_por_usuario_id de la oferta son inmutables';
    END IF;
    IF (OLD.horas_requeridas_snapshot, OLD.nota_minima_snapshot)
       IS DISTINCT FROM (NEW.horas_requeridas_snapshot, NEW.nota_minima_snapshot) THEN
        RAISE EXCEPTION 'Los snapshots académicos de la oferta no se editan directamente';
    END IF;
    IF (OLD.fecha_primera_publicacion, OLD.fecha_publicacion,
        OLD.resuelta_por_usuario_id, OLD.fecha_resolucion, OLD.created_at)
       IS DISTINCT FROM
       (NEW.fecha_primera_publicacion, NEW.fecha_publicacion,
        NEW.resuelta_por_usuario_id, NEW.fecha_resolucion, NEW.created_at) THEN
        RAISE EXCEPTION 'Las fechas y responsables de publicación/resolución son gestionados por el sistema';
    END IF;
    IF NEW.estado = OLD.estado AND NEW.motivo_decision IS DISTINCT FROM OLD.motivo_decision THEN
        RAISE EXCEPTION 'El motivo solo se registra al realizar una transición de estado';
    END IF;

    v_motivo_nuevo := NEW.motivo_decision IS DISTINCT FROM OLD.motivo_decision;

    -- cambios sustanciales: título, descripción, tipo, periodo, convenio, fechas
    v_sustancial :=
        (OLD.titulo, OLD.descripcion, OLD.tipo_practica_id, OLD.periodo_id, OLD.convenio_id,
         OLD.fecha_cierre, OLD.fecha_inicio_actividades, OLD.fecha_fin_actividades)
        IS DISTINCT FROM
        (NEW.titulo, NEW.descripcion, NEW.tipo_practica_id, NEW.periodo_id, NEW.convenio_id,
         NEW.fecha_cierre, NEW.fecha_inicio_actividades, NEW.fecha_fin_actividades);

    IF v_sustancial THEN
        IF OLD.tipo_practica_id IS DISTINCT FROM NEW.tipo_practica_id
           AND EXISTS (SELECT 1 FROM oferta_perfil p
                        JOIN postulacion po ON po.perfil_id = p.id
                       WHERE p.oferta_id = OLD.id) THEN
            RAISE EXCEPTION
                'No se puede cambiar el tipo de práctica cuando ya existen postulaciones; cree una nueva oferta';
        END IF;
        IF NOT puede_gestionar_oferta(v_actor, OLD.empresa_id, OLD.creada_por_usuario_id) THEN
            RAISE EXCEPTION 'Solo la empresa (ADMIN_EMPRESA, o el PUBLICADOR creador) puede editar la oferta';
        END IF;
        IF OLD.estado IN ('BORRADOR','RECHAZADA') THEN
            NULL;   -- edición libre
        ELSIF OLD.estado = 'PUBLICADA' AND NEW.estado = 'PUBLICADA' THEN
            NEW.estado := 'PENDIENTE_REVISION';   -- vuelve a revisión y deja de recibir postulaciones
            v_auto := TRUE;
        ELSIF OLD.estado = 'PUBLICADA' AND NEW.estado = 'PENDIENTE_REVISION' THEN
            NULL;
        ELSE
            RAISE EXCEPTION
                'No se pueden modificar los datos de una oferta en estado % (retírela a BORRADOR primero)',
                OLD.estado;
        END IF;
    END IF;

    IF NEW.estado = OLD.estado THEN RETURN NEW; END IF;

    IF NOT (
           (OLD.estado = 'BORRADOR'           AND NEW.estado IN ('PENDIENTE_REVISION','CANCELADA'))
        OR (OLD.estado = 'PENDIENTE_REVISION' AND NEW.estado IN ('PUBLICADA','RECHAZADA','BORRADOR','CANCELADA'))
        OR (OLD.estado = 'RECHAZADA'          AND NEW.estado IN ('BORRADOR','PENDIENTE_REVISION','CANCELADA'))
        OR (OLD.estado = 'PUBLICADA'          AND NEW.estado IN ('CERRADA','PENDIENTE_REVISION','CANCELADA'))
    ) THEN
        RAISE EXCEPTION 'Transición de oferta inválida: % -> %', OLD.estado, NEW.estado;
    END IF;

    -- ---------- autorización por acción ----------
    IF NEW.estado = 'CANCELADA' THEN
        IF NOT (tiene_rol_activo(v_actor, 'COORDINADOR')
                OR COALESCE(empresa_rol_activo(v_actor, OLD.empresa_id), '') = 'ADMIN_EMPRESA') THEN
            RAISE EXCEPTION 'Solo un COORDINADOR o el ADMIN_EMPRESA de la empresa puede cancelar la oferta';
        END IF;
    ELSIF NEW.estado = 'CERRADA' THEN
        IF NOT (tiene_rol_activo(v_actor, 'COORDINADOR')
                OR (v_actor IS NULL AND praxis_sistema())) THEN
            RAISE EXCEPTION 'Solo un COORDINADOR o un proceso del sistema puede cerrar la oferta';
        END IF;
    ELSIF NEW.estado IN ('PUBLICADA','RECHAZADA') THEN
        IF NOT tiene_rol_activo(v_actor, 'COORDINADOR') THEN
            RAISE EXCEPTION
                'Solo un COORDINADOR activo puede aprobar o rechazar la oferta';
        END IF;
    ELSE
        -- PENDIENTE_REVISION (envío o re-revisión) y BORRADOR (retiro): empresa
        IF NOT puede_gestionar_oferta(v_actor, OLD.empresa_id, OLD.creada_por_usuario_id) THEN
            RAISE EXCEPTION
                'Solo la empresa (ADMIN_EMPRESA, o el PUBLICADOR creador) puede enviar o retirar la oferta';
        END IF;
    END IF;

    -- motivo: pertenece a la última transición
    IF v_auto THEN
        NEW.motivo_decision := 'Modificación sustancial de una oferta publicada';
    ELSIF NOT v_motivo_nuevo THEN
        NEW.motivo_decision := NULL;
    END IF;
    IF NEW.estado IN ('RECHAZADA','CANCELADA')
       AND (NEW.motivo_decision IS NULL OR length(btrim(NEW.motivo_decision)) = 0) THEN
        RAISE EXCEPTION 'Rechazar o cancelar una oferta exige un motivo';
    END IF;

    -- revalidación al enviar a revisión (desde BORRADOR/RECHAZADA) y al aprobar
    v_revalidar := NEW.estado = 'PUBLICADA'
                   OR (NEW.estado = 'PENDIENTE_REVISION' AND OLD.estado <> 'PUBLICADA');
    IF v_revalidar THEN
        SELECT activa INTO v_empresa_activa FROM empresa WHERE id = NEW.empresa_id;
        IF NOT v_empresa_activa THEN
            RAISE EXCEPTION 'La empresa está inactiva';
        END IF;

        SELECT estado_efectivo, fecha_inicio, fecha_fin
          INTO v_conv_estado, v_conv_inicio, v_conv_fin
          FROM v_convenio_estado WHERE id = NEW.convenio_id;
        IF v_conv_estado IS DISTINCT FROM 'VIGENTE' THEN
            RAISE EXCEPTION 'El convenio de la oferta no está vigente (estado: %)', v_conv_estado;
        END IF;
        IF NEW.fecha_inicio_actividades < v_conv_inicio
           OR NEW.fecha_fin_actividades > v_conv_fin THEN
            RAISE EXCEPTION
                'Las actividades (% a %) deben estar dentro de la vigencia del convenio (% a %)',
                NEW.fecha_inicio_actividades, NEW.fecha_fin_actividades,
                v_conv_inicio, v_conv_fin;
        END IF;

        SELECT activo INTO v_tipo_activo FROM tipo_practica WHERE id = NEW.tipo_practica_id;
        IF NOT v_tipo_activo THEN
            RAISE EXCEPTION 'El tipo de práctica no está habilitado';
        END IF;
        -- Congelar horas y nota mínima del catálogo al publicar. Si ya hay postulaciones,
        -- se conserva el snapshot original aunque el catálogo haya cambiado después.
        IF NOT EXISTS (SELECT 1 FROM oferta_perfil p
                        JOIN postulacion po ON po.perfil_id = p.id
                       WHERE p.oferta_id = NEW.id) THEN
            SELECT horas_requeridas, nota_minima
              INTO v_tipo_horas, v_tipo_nota
              FROM tipo_practica WHERE id = NEW.tipo_practica_id;
            NEW.horas_requeridas_snapshot := v_tipo_horas;
            NEW.nota_minima_snapshot := v_tipo_nota;
        ELSIF OLD.horas_requeridas_snapshot IS NOT NULL THEN
            NEW.horas_requeridas_snapshot := OLD.horas_requeridas_snapshot;
            NEW.nota_minima_snapshot := OLD.nota_minima_snapshot;
        ELSE
            RAISE EXCEPTION 'La oferta tiene postulaciones pero carece del snapshot académico original';
        END IF;

        SELECT fecha_inicio, fecha_fin, activo
          INTO v_per_ini, v_per_fin, v_periodo_activo
          FROM periodo_academico WHERE id = NEW.periodo_id;
        IF NOT v_periodo_activo THEN
            RAISE EXCEPTION 'El periodo académico de la oferta no está activo';
        END IF;
        IF v_per_fin < v_hoy THEN
            RAISE EXCEPTION 'El periodo académico ya finalizó (%)', v_per_fin;
        END IF;
        IF NEW.fecha_inicio_actividades < v_per_ini OR NEW.fecha_fin_actividades > v_per_fin THEN
            RAISE EXCEPTION
                'Las actividades (% a %) deben estar dentro del periodo académico (% a %)',
                NEW.fecha_inicio_actividades, NEW.fecha_fin_actividades, v_per_ini, v_per_fin;
        END IF;

        IF NEW.fecha_cierre < v_hoy THEN
            RAISE EXCEPTION 'La fecha de cierre (%) ya pasó', NEW.fecha_cierre;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM oferta_perfil WHERE oferta_id = NEW.id AND plazas > 0) THEN
            RAISE EXCEPTION 'La oferta debe tener al menos un perfil con plazas';
        END IF;
    END IF;

    -- resolución del coordinador y fechas de publicación
    IF NEW.estado IN ('PUBLICADA','RECHAZADA') THEN
        NEW.resuelta_por_usuario_id := v_actor;
        NEW.fecha_resolucion := NOW();
        IF NEW.estado = 'PUBLICADA' THEN
            NEW.fecha_publicacion        := NOW();                                   -- última (re)publicación
            NEW.fecha_primera_publicacion := COALESCE(OLD.fecha_primera_publicacion, NOW());
        END IF;
    ELSIF NEW.estado = 'CERRADA' THEN
        -- Puede cerrarla el coordinador al cubrir cupos o el proceso automático al vencer.
        NEW.fecha_resolucion := NOW();
        NEW.resuelta_por_usuario_id := v_actor;
    ELSIF NEW.estado IN ('BORRADOR','PENDIENTE_REVISION') THEN
        NEW.resuelta_por_usuario_id := NULL;
        NEW.fecha_resolucion := NULL;
    END IF;

    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_oferta
    BEFORE INSERT OR UPDATE ON oferta_practica
    FOR EACH ROW EXECUTE FUNCTION validar_oferta();

CREATE OR REPLACE FUNCTION auditar_cambios_cabecera_oferta()
RETURNS TRIGGER AS $$
BEGIN
    IF (OLD.titulo, OLD.descripcion, OLD.tipo_practica_id, OLD.periodo_id,
        OLD.convenio_id, OLD.fecha_inicio_actividades, OLD.fecha_fin_actividades,
        OLD.fecha_cierre, OLD.horas_requeridas_snapshot, OLD.nota_minima_snapshot)
       IS DISTINCT FROM
       (NEW.titulo, NEW.descripcion, NEW.tipo_practica_id, NEW.periodo_id,
        NEW.convenio_id, NEW.fecha_inicio_actividades, NEW.fecha_fin_actividades,
        NEW.fecha_cierre, NEW.horas_requeridas_snapshot, NEW.nota_minima_snapshot) THEN
        IF praxis_actor() IS NULL THEN
            RAISE EXCEPTION 'Falta praxis.usuario_id para auditar cambios en la oferta';
        END IF;
        INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle)
        VALUES (praxis_actor(), 'OFERTA_CABECERA_MODIFICADA', 'oferta_practica', NEW.id,
                jsonb_build_object('antes', jsonb_build_object(
                    'titulo', OLD.titulo, 'descripcion', OLD.descripcion,
                    'tipo_practica_id', OLD.tipo_practica_id, 'periodo_id', OLD.periodo_id,
                    'convenio_id', OLD.convenio_id, 'fecha_inicio_actividades', OLD.fecha_inicio_actividades,
                    'fecha_fin_actividades', OLD.fecha_fin_actividades, 'fecha_cierre', OLD.fecha_cierre,
                    'horas_requeridas_snapshot', OLD.horas_requeridas_snapshot,
                    'nota_minima_snapshot', OLD.nota_minima_snapshot),
                    'despues', jsonb_build_object(
                    'titulo', NEW.titulo, 'descripcion', NEW.descripcion,
                    'tipo_practica_id', NEW.tipo_practica_id, 'periodo_id', NEW.periodo_id,
                    'convenio_id', NEW.convenio_id, 'fecha_inicio_actividades', NEW.fecha_inicio_actividades,
                    'fecha_fin_actividades', NEW.fecha_fin_actividades, 'fecha_cierre', NEW.fecha_cierre,
                    'horas_requeridas_snapshot', NEW.horas_requeridas_snapshot,
                    'nota_minima_snapshot', NEW.nota_minima_snapshot)));
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditar_cambios_cabecera_oferta
    AFTER UPDATE ON oferta_practica
    FOR EACH ROW EXECUTE FUNCTION auditar_cambios_cabecera_oferta();

-- 7.4 oferta_practica: no se borra (se cancela)
CREATE OR REPLACE FUNCTION proteger_borrado_oferta()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Las ofertas no se borran; use CANCELADA';
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_borrado_oferta
    BEFORE DELETE ON oferta_practica
    FOR EACH ROW EXECUTE FUNCTION proteger_borrado_oferta();

-- 7.5 oferta_estado_historial: automático, con responsable obligatorio; inmutable
CREATE OR REPLACE FUNCTION registrar_historial_oferta()
RETURNS TRIGGER AS $$
DECLARE
    v_usuario BIGINT  := praxis_actor();
    v_sistema BOOLEAN := praxis_sistema();
    v_motivo  TEXT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO oferta_estado_historial
            (oferta_id, estado_anterior, estado_nuevo, usuario_id, es_sistema, motivo)
        VALUES (NEW.id, NULL, NEW.estado, COALESCE(v_usuario, NEW.creada_por_usuario_id), FALSE,
                NULLIF(current_setting('praxis.motivo', TRUE), ''));
    ELSIF NEW.estado IS DISTINCT FROM OLD.estado THEN
        IF v_usuario IS NULL AND NOT v_sistema THEN
            RAISE EXCEPTION
                'Cambio de estado de oferta sin responsable: fije praxis.usuario_id (o praxis.sistema)';
        END IF;
        v_motivo := COALESCE(NULLIF(btrim(NEW.motivo_decision), ''),
                             NULLIF(current_setting('praxis.motivo', TRUE), ''));
        INSERT INTO oferta_estado_historial
            (oferta_id, estado_anterior, estado_nuevo, usuario_id, es_sistema, motivo)
        VALUES (NEW.id, OLD.estado, NEW.estado, v_usuario, v_usuario IS NULL, v_motivo);
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_registrar_historial_oferta
    AFTER INSERT OR UPDATE OF estado ON oferta_practica
    FOR EACH ROW EXECUTE FUNCTION registrar_historial_oferta();

CREATE OR REPLACE FUNCTION proteger_historial_inmutable()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '% es inmutable', TG_TABLE_NAME;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_historial_oferta
    BEFORE UPDATE OR DELETE ON oferta_estado_historial
    FOR EACH ROW EXECUTE FUNCTION proteger_historial_inmutable();
CREATE TRIGGER trg_proteger_historial_recomendacion
    BEFORE UPDATE OR DELETE ON postulacion_recomendacion_historial
    FOR EACH ROW EXECUTE FUNCTION proteger_historial_inmutable();

-- 7.6 perfiles, carreras y requisitos (BEFORE: bloquea la oferta ANTES de tocar la fila hija)
--     * perfil_id / oferta_id inmutables
--     * solo editables en BORRADOR / RECHAZADA / PUBLICADA, por la empresa autorizada
--     * si la oferta está PUBLICADA, el cambio la devuelve a PENDIENTE_REVISION
--     * las carreras asignadas deben estar activas
CREATE OR REPLACE FUNCTION oferta_hijo_modificado()
RETURNS TRIGGER AS $$
DECLARE
    v_oferta  BIGINT;
    v_perfil  BIGINT;
    v_estado  VARCHAR(20);
    v_empresa BIGINT;
    v_creador BIGINT;
    v_actor   BIGINT := praxis_actor();
BEGIN
    IF TG_TABLE_NAME = 'oferta_perfil' THEN
        IF TG_OP = 'DELETE' THEN
            v_oferta := OLD.oferta_id;
        ELSE
            v_oferta := NEW.oferta_id;
            IF TG_OP = 'UPDATE' THEN
                IF NEW.oferta_id IS DISTINCT FROM OLD.oferta_id THEN
                    RAISE EXCEPTION 'Un perfil no puede cambiar de oferta';
                END IF;
                IF (OLD.nombre, OLD.descripcion, OLD.plazas)
                   IS NOT DISTINCT FROM (NEW.nombre, NEW.descripcion, NEW.plazas) THEN
                    RETURN NEW;
                END IF;
            END IF;
        END IF;
    ELSE
        IF TG_OP = 'UPDATE' THEN
            IF NEW.perfil_id IS DISTINCT FROM OLD.perfil_id THEN
                RAISE EXCEPTION '% no puede moverse a otro perfil', TG_TABLE_NAME;
            END IF;
            IF TG_TABLE_NAME = 'oferta_perfil_requisito'
               AND (OLD.descripcion, OLD.obligatorio)
                   IS NOT DISTINCT FROM (NEW.descripcion, NEW.obligatorio) THEN
                RETURN NEW;
            END IF;
        END IF;
        IF TG_OP = 'DELETE' THEN v_perfil := OLD.perfil_id; ELSE v_perfil := NEW.perfil_id; END IF;
        SELECT oferta_id INTO v_oferta FROM oferta_perfil WHERE id = v_perfil;
    END IF;

    IF v_oferta IS NULL THEN   -- perfil ya eliminado (borrado en cascada)
        IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
    END IF;

    -- 1) bloquear la oferta (primer eslabón del orden de bloqueos)
    SELECT estado, empresa_id, creada_por_usuario_id
      INTO v_estado, v_empresa, v_creador
      FROM oferta_practica WHERE id = v_oferta FOR UPDATE;

    IF v_estado NOT IN ('BORRADOR','RECHAZADA','PUBLICADA') THEN
        RAISE EXCEPTION
            'No se pueden modificar perfiles, carreras ni requisitos de una oferta en estado % (retírela a BORRADOR primero)',
            v_estado;
    END IF;
    IF NOT puede_gestionar_oferta(v_actor, v_empresa, v_creador) THEN
        RAISE EXCEPTION
            'Solo la empresa (ADMIN_EMPRESA, o el PUBLICADOR creador) puede modificar perfiles, carreras y requisitos';
    END IF;

    IF TG_TABLE_NAME = 'oferta_perfil_carrera' AND TG_OP <> 'DELETE' THEN
        IF NOT EXISTS (SELECT 1 FROM carrera WHERE id = NEW.carrera_id AND activa) THEN
            RAISE EXCEPTION 'La carrera asignada al perfil no está habilitada';
        END IF;
    END IF;

    IF v_estado = 'PUBLICADA' THEN
        UPDATE oferta_practica
           SET estado = 'PENDIENTE_REVISION',
               motivo_decision = 'Modificación sustancial: cambios en perfiles, plazas, carreras o requisitos'
         WHERE id = v_oferta;
        INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle)
        VALUES (v_actor, 'OFERTA_MODIFICACION_SUSTANCIAL', 'oferta_practica', v_oferta,
                jsonb_build_object('tabla', TG_TABLE_NAME, 'operacion', TG_OP));
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_oferta_perfil_modificado
    BEFORE INSERT OR UPDATE OR DELETE ON oferta_perfil
    FOR EACH ROW EXECUTE FUNCTION oferta_hijo_modificado();
CREATE TRIGGER trg_oferta_perfil_carrera_modificado
    BEFORE INSERT OR UPDATE OR DELETE ON oferta_perfil_carrera
    FOR EACH ROW EXECUTE FUNCTION oferta_hijo_modificado();
CREATE TRIGGER trg_oferta_perfil_requisito_modificado
    BEFORE INSERT OR UPDATE OR DELETE ON oferta_perfil_requisito
    FOR EACH ROW EXECUTE FUNCTION oferta_hijo_modificado();

-- Historial de cambios sustanciales de perfiles, carreras y requisitos.
-- Permite reconstruir las condiciones de una convocatoria, incluso tras republicarla.
CREATE TABLE oferta_perfil_cambio_historial (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oferta_id   BIGINT REFERENCES oferta_practica(id) ON DELETE RESTRICT, -- NULL solo si el hijo se elimina en cascada
    perfil_id   BIGINT,
    tabla       VARCHAR(40) NOT NULL,
    operacion   VARCHAR(10) NOT NULL,
    registro_id BIGINT,
    antes       JSONB,
    despues     JSONB,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_oferta_perfil_hist_operacion CHECK (operacion IN ('INSERT','UPDATE','DELETE'))
);
CREATE INDEX idx_oferta_perfil_hist_oferta_fecha
    ON oferta_perfil_cambio_historial(oferta_id, fecha DESC);

CREATE OR REPLACE FUNCTION registrar_cambio_oferta_hijo()
RETURNS TRIGGER AS $$
DECLARE
    v_perfil BIGINT;
    v_oferta BIGINT;
    v_id BIGINT;
    v_antes JSONB;
    v_despues JSONB;
BEGIN
    IF praxis_actor() IS NULL THEN
        RAISE EXCEPTION 'Falta praxis.usuario_id para auditar el cambio del perfil';
    END IF;
    IF TG_OP = 'DELETE' THEN
        v_antes := to_jsonb(OLD);
        v_id := CASE WHEN TG_TABLE_NAME = 'oferta_perfil' THEN OLD.id
                     WHEN TG_TABLE_NAME = 'oferta_perfil_carrera' THEN NULL
                     ELSE OLD.id END;
        IF TG_TABLE_NAME = 'oferta_perfil' THEN
            v_perfil := OLD.id; v_oferta := OLD.oferta_id;
        ELSE
            v_perfil := OLD.perfil_id;
            SELECT oferta_id INTO v_oferta FROM oferta_perfil WHERE id = v_perfil;
        END IF;
    ELSIF TG_OP = 'INSERT' THEN
        v_despues := to_jsonb(NEW);
        v_id := CASE WHEN TG_TABLE_NAME = 'oferta_perfil_carrera' THEN NULL ELSE NEW.id END;
        IF TG_TABLE_NAME = 'oferta_perfil' THEN
            v_perfil := NEW.id; v_oferta := NEW.oferta_id;
        ELSE
            v_perfil := NEW.perfil_id;
            SELECT oferta_id INTO v_oferta FROM oferta_perfil WHERE id = v_perfil;
        END IF;
    ELSE
        v_antes := to_jsonb(OLD); v_despues := to_jsonb(NEW);
        v_id := CASE WHEN TG_TABLE_NAME = 'oferta_perfil_carrera' THEN NULL ELSE NEW.id END;
        IF TG_TABLE_NAME = 'oferta_perfil' THEN
            v_perfil := NEW.id; v_oferta := NEW.oferta_id;
        ELSE
            v_perfil := NEW.perfil_id;
            SELECT oferta_id INTO v_oferta FROM oferta_perfil WHERE id = v_perfil;
        END IF;
    END IF;
    INSERT INTO oferta_perfil_cambio_historial
        (oferta_id, perfil_id, tabla, operacion, registro_id, antes, despues, usuario_id)
    VALUES (v_oferta, v_perfil, TG_TABLE_NAME, TG_OP, v_id, v_antes, v_despues, praxis_actor());
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_hist_oferta_perfil
    AFTER INSERT OR UPDATE OR DELETE ON oferta_perfil
    FOR EACH ROW EXECUTE FUNCTION registrar_cambio_oferta_hijo();
CREATE TRIGGER trg_hist_oferta_perfil_carrera
    AFTER INSERT OR UPDATE OR DELETE ON oferta_perfil_carrera
    FOR EACH ROW EXECUTE FUNCTION registrar_cambio_oferta_hijo();
CREATE TRIGGER trg_hist_oferta_perfil_requisito
    AFTER INSERT OR UPDATE OR DELETE ON oferta_perfil_requisito
    FOR EACH ROW EXECUTE FUNCTION registrar_cambio_oferta_hijo();

CREATE TRIGGER trg_inmutable_hist_oferta_perfil
    BEFORE UPDATE OR DELETE ON oferta_perfil_cambio_historial
    FOR EACH ROW EXECUTE FUNCTION proteger_historial_inmutable();

-- 7.7 oferta_perfil: no reducir plazas bajo las ya aceptadas
--     (un perfil con postulaciones no puede eliminarse: FK RESTRICT en postulacion)
CREATE OR REPLACE FUNCTION validar_perfil_plazas()
RETURNS TRIGGER AS $$
DECLARE v_aceptadas INT;
BEGIN
    IF NEW.plazas < OLD.plazas THEN
        -- 2) bloquear el perfil y recién entonces contar (serializa con aceptaciones)
        PERFORM 1 FROM oferta_perfil WHERE id = OLD.id FOR UPDATE;
        SELECT COUNT(*) INTO v_aceptadas
          FROM postulacion WHERE perfil_id = NEW.id AND estado = 'ACEPTADA';
        IF NEW.plazas < v_aceptadas THEN
            RAISE EXCEPTION
                'No se pueden reducir las plazas a %: ya hay % aceptados', NEW.plazas, v_aceptadas;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_perfil_plazas
    BEFORE UPDATE ON oferta_perfil
    FOR EACH ROW EXECUTE FUNCTION validar_perfil_plazas();

-- 7.8 postulacion: identidad del actor, recomendación, decisión académica, cupos
CREATE OR REPLACE FUNCTION validar_postulacion()
RETURNS TRIGGER AS $$
DECLARE
    v_hoy         DATE := (NOW() AT TIME ZONE 'America/Guayaquil')::date;
    v_actor       BIGINT := praxis_actor();
    v_oferta_id   BIGINT;
    v_estado_of   VARCHAR(20);
    v_cierre      DATE;
    v_empresa     BIGINT;
    v_convenio    BIGINT;
    v_periodo     BIGINT;
    v_plazas      INT;
    v_aceptadas   INT;
    v_carrera     BIGINT;
    v_conv_estado TEXT;
    v_conv_inicio DATE;
    v_conv_fin    DATE;
    v_inicio_actividades DATE;
    v_fin_actividades    DATE;
    v_empresa_activa BOOLEAN;
    v_tipo_activo   BOOLEAN;
    v_periodo_activo BOOLEAN;
    v_carrera_activa BOOLEAN;
    v_membresia   BIGINT;
    v_motivo_nuevo BOOLEAN;
    v_correccion  BOOLEAN;
BEGIN
    SELECT oferta_id INTO v_oferta_id FROM oferta_perfil WHERE id = NEW.perfil_id;

    -- 1) bloquear la oferta y leer su estado YA bloqueada (aceptar = exclusivo; resto = compartido)
    IF TG_OP = 'UPDATE' AND NEW.estado = 'ACEPTADA' AND OLD.estado <> 'ACEPTADA' THEN
        SELECT o.estado, o.fecha_cierre, o.empresa_id, o.convenio_id, o.periodo_id,
               o.fecha_inicio_actividades, o.fecha_fin_actividades
          INTO v_estado_of, v_cierre, v_empresa, v_convenio, v_periodo,
               v_inicio_actividades, v_fin_actividades
          FROM oferta_practica o WHERE o.id = v_oferta_id FOR UPDATE;
    ELSE
        SELECT o.estado, o.fecha_cierre, o.empresa_id, o.convenio_id, o.periodo_id,
               o.fecha_inicio_actividades, o.fecha_fin_actividades
          INTO v_estado_of, v_cierre, v_empresa, v_convenio, v_periodo,
               v_inicio_actividades, v_fin_actividades
          FROM oferta_practica o WHERE o.id = v_oferta_id FOR SHARE;
    END IF;

    SELECT estado_efectivo, fecha_inicio, fecha_fin
      INTO v_conv_estado, v_conv_inicio, v_conv_fin
      FROM v_convenio_estado WHERE id = v_convenio;

    -- ---------- INSERT ----------
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'PENDIENTE' OR NEW.recomendacion_empresa <> 'SIN_EVALUAR' THEN
            RAISE EXCEPTION 'Una postulación nueva debe iniciar PENDIENTE y SIN_EVALUAR';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM estudiante e JOIN usuario u ON u.id = e.usuario_id
                        WHERE e.id = NEW.estudiante_id AND e.usuario_id = v_actor AND u.activo) THEN
            RAISE EXCEPTION 'Solo el propio estudiante autenticado puede postularse';
        END IF;
        IF v_estado_of IS DISTINCT FROM 'PUBLICADA' THEN
            RAISE EXCEPTION 'La oferta no acepta postulaciones (estado: %)', v_estado_of;
        END IF;
        IF v_cierre < v_hoy THEN
            RAISE EXCEPTION 'La oferta ya cerró el plazo de postulación (%)', v_cierre;
        END IF;
        IF v_conv_estado IS DISTINCT FROM 'VIGENTE' THEN
            RAISE EXCEPTION 'El convenio de la oferta no está vigente (estado: %)', v_conv_estado;
        END IF;
        IF v_inicio_actividades < v_conv_inicio OR v_fin_actividades > v_conv_fin THEN
            RAISE EXCEPTION 'Las actividades de la oferta exceden la vigencia del convenio';
        END IF;
        SELECT activa INTO v_empresa_activa FROM empresa WHERE id = v_empresa;
        SELECT activo INTO v_tipo_activo FROM tipo_practica
         WHERE id = (SELECT tipo_practica_id FROM oferta_practica WHERE id = v_oferta_id);
        SELECT activo INTO v_periodo_activo FROM periodo_academico WHERE id = v_periodo;
        IF NOT v_empresa_activa OR NOT v_tipo_activo OR NOT v_periodo_activo THEN
            RAISE EXCEPTION 'La empresa, el tipo de práctica o el periodo ya no están habilitados';
        END IF;

        SELECT e.carrera_id, c.activa INTO v_carrera, v_carrera_activa
          FROM estudiante e JOIN carrera c ON c.id = e.carrera_id
         WHERE e.id = NEW.estudiante_id;
        IF NOT v_carrera_activa THEN
            RAISE EXCEPTION 'La carrera del estudiante no está habilitada';
        END IF;
        IF EXISTS (SELECT 1 FROM oferta_perfil_carrera WHERE perfil_id = NEW.perfil_id)
           AND NOT EXISTS (SELECT 1 FROM oferta_perfil_carrera
                            WHERE perfil_id = NEW.perfil_id AND carrera_id = v_carrera) THEN
            RAISE EXCEPTION 'El perfil no está dirigido a la carrera del estudiante';
        END IF;

        -- perfil con plazas completas = cerrado
        SELECT plazas INTO v_plazas FROM oferta_perfil WHERE id = NEW.perfil_id;
        SELECT COUNT(*) INTO v_aceptadas
          FROM postulacion WHERE perfil_id = NEW.perfil_id AND estado = 'ACEPTADA';
        IF v_aceptadas >= v_plazas THEN
            RAISE EXCEPTION 'El perfil ya completó sus plazas y no recibe nuevas postulaciones';
        END IF;
        RETURN NEW;
    END IF;

    -- ---------- UPDATE ----------
    IF (OLD.perfil_id, OLD.estudiante_id, OLD.fecha_postulacion)
       IS DISTINCT FROM (NEW.perfil_id, NEW.estudiante_id, NEW.fecha_postulacion) THEN
        RAISE EXCEPTION 'perfil, estudiante y fecha de la postulación son inmutables';
    END IF;

    -- corrección administrativa explícita (coordinador/administrador + motivo), siempre auditada
    v_correccion :=
        COALESCE(current_setting('praxis.correccion_decision', TRUE), '') = 'true'
        AND (tiene_rol_activo(v_actor, 'COORDINADOR') OR tiene_rol_activo(v_actor, 'ADMINISTRADOR'))
        AND NULLIF(current_setting('praxis.motivo', TRUE), '') IS NOT NULL;

    IF OLD.estado <> 'PENDIENTE' THEN
        -- postulación ya resuelta: los datos de decisión y de recomendación quedan congelados
        IF (OLD.mensaje, OLD.recomendacion_empresa, OLD.observacion_empresa,
            OLD.evaluada_por_empresa_usuario_id, OLD.fecha_evaluacion_empresa,
            OLD.resuelta_por, OLD.fecha_resolucion)
           IS DISTINCT FROM
           (NEW.mensaje, NEW.recomendacion_empresa, NEW.observacion_empresa,
            NEW.evaluada_por_empresa_usuario_id, NEW.fecha_evaluacion_empresa,
            NEW.resuelta_por, NEW.fecha_resolucion)
           OR (NEW.estado = OLD.estado
               AND (OLD.motivo_decision, OLD.cancelada_por, OLD.fecha_cancelacion)
                   IS DISTINCT FROM
                   (NEW.motivo_decision, NEW.cancelada_por, NEW.fecha_cancelacion)) THEN
            IF NOT v_correccion OR NEW.estado <> OLD.estado THEN
                RAISE EXCEPTION
                    'Postulación % ya resuelta: sus datos de decisión no se modifican (use la corrección administrativa auditada)',
                    OLD.estado;
            END IF;
            INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle)
            VALUES (v_actor, 'POSTULACION_CORRECCION_ADMIN', 'postulacion', NEW.id,
                    jsonb_build_object('antes', to_jsonb(OLD), 'despues', to_jsonb(NEW),
                                       'motivo', current_setting('praxis.motivo', TRUE)));
        END IF;
        IF NEW.estado = OLD.estado THEN RETURN NEW; END IF;
    ELSE
        -- Mientras está pendiente, el estudiante solo puede editar su mensaje antes
        -- de que la empresa emita una recomendación. Los campos de resolución son internos.
        IF NEW.mensaje IS DISTINCT FROM OLD.mensaje THEN
            IF NEW.estado <> OLD.estado
               OR OLD.recomendacion_empresa <> 'SIN_EVALUAR'
               OR NOT EXISTS (
                   SELECT 1 FROM estudiante e JOIN usuario u ON u.id = e.usuario_id
                    WHERE e.id = OLD.estudiante_id AND e.usuario_id = v_actor AND u.activo) THEN
                RAISE EXCEPTION
                    'Solo el estudiante propietario puede editar el mensaje antes de la evaluación empresarial';
            END IF;
        END IF;
        IF NEW.estado = OLD.estado
           AND (OLD.resuelta_por, OLD.fecha_resolucion, OLD.motivo_decision,
                OLD.cancelada_por, OLD.fecha_cancelacion)
               IS DISTINCT FROM
               (NEW.resuelta_por, NEW.fecha_resolucion, NEW.motivo_decision,
                NEW.cancelada_por, NEW.fecha_cancelacion) THEN
            RAISE EXCEPTION 'Los campos de resolución solo cambian mediante una transición autorizada';
        END IF;

        -- PENDIENTE: recomendación empresarial (solo si la oferta sigue vigente para revisión)
        IF (OLD.recomendacion_empresa, OLD.observacion_empresa,
            OLD.evaluada_por_empresa_usuario_id, OLD.fecha_evaluacion_empresa)
           IS DISTINCT FROM
           (NEW.recomendacion_empresa, NEW.observacion_empresa,
            NEW.evaluada_por_empresa_usuario_id, NEW.fecha_evaluacion_empresa) THEN
            IF NEW.estado <> 'PENDIENTE' THEN
                RAISE EXCEPTION 'La recomendación no puede cambiar junto con la resolución de la postulación';
            END IF;
            IF v_estado_of NOT IN ('PUBLICADA','CERRADA') THEN
                RAISE EXCEPTION 'La oferta (estado %) no admite recomendaciones', v_estado_of;
            END IF;
            IF OLD.recomendacion_empresa <> 'SIN_EVALUAR'
               AND NEW.recomendacion_empresa = 'SIN_EVALUAR' THEN
                RAISE EXCEPTION
                    'Una recomendación registrada no puede borrarse ni restablecerse a SIN_EVALUAR';
            END IF;
            IF NEW.recomendacion_empresa = 'SIN_EVALUAR' THEN
                RAISE EXCEPTION 'La recomendación debe ser RECOMENDADO o NO_RECOMENDADO';
            END IF;
            v_membresia := empresa_membresia_id(v_actor, v_empresa);
            IF v_membresia IS NULL
               OR NEW.evaluada_por_empresa_usuario_id IS DISTINCT FROM v_membresia THEN
                RAISE EXCEPTION
                    'El evaluador debe ser el usuario autenticado, miembro activo de la empresa de la oferta';
            END IF;
            NEW.fecha_evaluacion_empresa := NOW();
        END IF;
        IF NEW.estado = OLD.estado THEN RETURN NEW; END IF;
    END IF;

    -- ---------- transiciones de estado ----------
    IF NOT (
           (OLD.estado = 'PENDIENTE' AND NEW.estado IN ('ACEPTADA','RECHAZADA','RETIRADA'))
        OR (OLD.estado = 'ACEPTADA'  AND NEW.estado = 'CANCELADA')
    ) THEN
        RAISE EXCEPTION 'Transición de postulación inválida: % -> %', OLD.estado, NEW.estado;
    END IF;

    -- motivo: pertenece a la última transición
    v_motivo_nuevo := NEW.motivo_decision IS DISTINCT FROM OLD.motivo_decision;
    IF NOT v_motivo_nuevo THEN NEW.motivo_decision := NULL; END IF;
    IF NEW.estado IN ('RECHAZADA','CANCELADA')
       AND (NEW.motivo_decision IS NULL OR length(btrim(NEW.motivo_decision)) = 0) THEN
        RAISE EXCEPTION 'Rechazar o cancelar una postulación exige un motivo';
    END IF;

    -- RETIRADA: únicamente el estudiante propietario
    IF NEW.estado = 'RETIRADA' THEN
        IF NOT EXISTS (SELECT 1 FROM estudiante e JOIN usuario u ON u.id = e.usuario_id
                        WHERE e.id = NEW.estudiante_id AND e.usuario_id = v_actor AND u.activo) THEN
            RAISE EXCEPTION 'Solo el estudiante propietario puede retirar su postulación';
        END IF;
    END IF;

    -- ACEPTADA / RECHAZADA: coordinador activo que es quien ejecuta la operación
    IF NEW.estado IN ('ACEPTADA','RECHAZADA') THEN
        IF NOT tiene_rol_activo(v_actor, 'COORDINADOR')
           OR NEW.resuelta_por IS DISTINCT FROM v_actor THEN
            RAISE EXCEPTION
                'El usuario autenticado debe ser un COORDINADOR activo y ser quien resuelve la postulación';
        END IF;
        NEW.fecha_resolucion := NOW();
    END IF;

    -- CANCELADA: coordinador, o sincronización desde la práctica cancelada/rechazada
    IF NEW.estado = 'CANCELADA' THEN
        IF v_actor IS NULL THEN
            RAISE EXCEPTION 'Falta praxis.usuario_id: no se identifica quién cancela la postulación';
        END IF;
        IF NOT (tiene_rol_activo(v_actor, 'COORDINADOR')
                OR COALESCE(current_setting('praxis.sync_practica', TRUE), '') = 'true') THEN
            RAISE EXCEPTION 'Solo un COORDINADOR activo puede cancelar una postulación aceptada';
        END IF;
        IF EXISTS (SELECT 1 FROM practica
                    WHERE postulacion_id = NEW.id
                      AND estado NOT IN ('CANCELADA','RECHAZADA')) THEN
            RAISE EXCEPTION 'Resuelva primero la práctica asociada (debe estar CANCELADA o RECHAZADA)';
        END IF;
        NEW.cancelada_por := v_actor;
        NEW.fecha_cancelacion := NOW();
    END IF;

    IF NEW.estado = 'ACEPTADA' THEN
        -- la oferta ya está bloqueada y su estado releído
        IF v_estado_of NOT IN ('PUBLICADA','CERRADA') THEN
            RAISE EXCEPTION 'No se puede aceptar postulaciones en una oferta %', v_estado_of;
        END IF;
        IF v_conv_estado IS DISTINCT FROM 'VIGENTE' THEN
            RAISE EXCEPTION 'El convenio de la oferta no está vigente (estado: %)', v_conv_estado;
        END IF;
        IF v_inicio_actividades < v_conv_inicio OR v_fin_actividades > v_conv_fin THEN
            RAISE EXCEPTION 'Las actividades de la oferta exceden la vigencia del convenio';
        END IF;
        SELECT activa INTO v_empresa_activa FROM empresa WHERE id = v_empresa;
        SELECT activo INTO v_tipo_activo FROM tipo_practica
         WHERE id = (SELECT tipo_practica_id FROM oferta_practica WHERE id = v_oferta_id);
        SELECT activo INTO v_periodo_activo FROM periodo_academico WHERE id = v_periodo;
        IF NOT v_empresa_activa OR NOT v_tipo_activo OR NOT v_periodo_activo THEN
            RAISE EXCEPTION 'La empresa, el tipo de práctica o el periodo ya no están habilitados';
        END IF;

        -- 2) bloquear el perfil  3) releer plazas  4) contar aceptadas  5) verificar cupo
        SELECT plazas INTO v_plazas FROM oferta_perfil WHERE id = NEW.perfil_id FOR UPDATE;
        SELECT COUNT(*) INTO v_aceptadas
          FROM postulacion
         WHERE perfil_id = NEW.perfil_id AND estado = 'ACEPTADA' AND id <> NEW.id;
        IF v_aceptadas >= v_plazas THEN
            RAISE EXCEPTION 'No hay plazas disponibles en el perfil (plazas=%)', v_plazas;
        END IF;

        -- el estudiante no puede tener otra práctica viva en el mismo periodo
        IF EXISTS (SELECT 1 FROM practica
                    WHERE estudiante_id = NEW.estudiante_id
                      AND periodo_id = v_periodo
                      AND estado NOT IN ('RECHAZADA','CANCELADA','REPROBADA')
                      AND postulacion_id IS DISTINCT FROM NEW.id) THEN
            RAISE EXCEPTION 'El estudiante ya tiene una práctica activa en este periodo';
        END IF;
    END IF;

    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_postulacion
    BEFORE INSERT OR UPDATE ON postulacion
    FOR EACH ROW EXECUTE FUNCTION validar_postulacion();

-- 7.9 postulacion: auditoría de cada transición (actor siempre identificado)
CREATE OR REPLACE FUNCTION auditar_postulacion()
RETURNS TRIGGER AS $$
BEGIN
    IF praxis_actor() IS NULL THEN
        RAISE EXCEPTION 'Falta praxis.usuario_id: toda transición de postulación exige un actor';
    END IF;
    INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle)
    VALUES (praxis_actor(), 'POSTULACION_' || NEW.estado, 'postulacion', NEW.id,
            jsonb_build_object('estado_anterior', OLD.estado, 'estado_nuevo', NEW.estado,
                               'motivo', NEW.motivo_decision));
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditar_postulacion
    AFTER UPDATE OF estado ON postulacion
    FOR EACH ROW WHEN (OLD.estado IS DISTINCT FROM NEW.estado)
    EXECUTE FUNCTION auditar_postulacion();

CREATE OR REPLACE FUNCTION auditar_creacion_postulacion()
RETURNS TRIGGER AS $$
BEGIN
    IF praxis_actor() IS NULL OR praxis_actor() IS DISTINCT FROM
       (SELECT usuario_id FROM estudiante WHERE id = NEW.estudiante_id) THEN
        RAISE EXCEPTION 'La creación de la postulación debe quedar asociada al estudiante autenticado';
    END IF;
    INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle)
    VALUES (praxis_actor(), 'POSTULACION_CREADA', 'postulacion', NEW.id,
            jsonb_build_object('perfil_id', NEW.perfil_id,
                               'estudiante_id', NEW.estudiante_id,
                               'fecha_postulacion', NEW.fecha_postulacion));
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditar_creacion_postulacion
    AFTER INSERT ON postulacion
    FOR EACH ROW EXECUTE FUNCTION auditar_creacion_postulacion();

-- 7.10 historial de recomendaciones empresariales
CREATE OR REPLACE FUNCTION registrar_recomendacion_historial()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO postulacion_recomendacion_historial
        (postulacion_id, recomendacion, observacion, empresa_usuario_id, usuario_id)
    VALUES (NEW.id, NEW.recomendacion_empresa, NEW.observacion_empresa,
            NEW.evaluada_por_empresa_usuario_id, praxis_actor());
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_registrar_recomendacion_historial
    AFTER UPDATE OF recomendacion_empresa, observacion_empresa ON postulacion
    FOR EACH ROW WHEN (NEW.recomendacion_empresa <> 'SIN_EVALUAR'
                       AND (OLD.recomendacion_empresa, OLD.observacion_empresa)
                           IS DISTINCT FROM (NEW.recomendacion_empresa, NEW.observacion_empresa))
    EXECUTE FUNCTION registrar_recomendacion_historial();

-- 7.11 al aceptar la última plaza de TODOS los perfiles, la oferta pasa a CERRADA
--      (si queda algún perfil con plazas, la oferta continúa publicada)
CREATE OR REPLACE FUNCTION cerrar_oferta_si_completa()
RETURNS TRIGGER AS $$
DECLARE v_oferta BIGINT;
BEGIN
    SELECT oferta_id INTO v_oferta FROM oferta_perfil WHERE id = NEW.perfil_id;
    IF NOT EXISTS (SELECT 1 FROM v_oferta_perfil_cupo
                    WHERE oferta_id = v_oferta AND NOT perfil_cerrado) THEN
        UPDATE oferta_practica
           SET estado = 'CERRADA',
               motivo_decision = 'Todas las plazas de la convocatoria fueron cubiertas'
         WHERE id = v_oferta AND estado = 'PUBLICADA';
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cerrar_oferta_si_completa
    AFTER UPDATE OF estado ON postulacion
    FOR EACH ROW WHEN (NEW.estado = 'ACEPTADA' AND OLD.estado IS DISTINCT FROM NEW.estado)
    EXECUTE FUNCTION cerrar_oferta_si_completa();

-- 7.12 una postulación ACEPTADA debe tener su práctica en la MISMA transacción,
--      y esa práctica no puede estar ya cancelada/rechazada (se verifica al COMMIT)
CREATE OR REPLACE FUNCTION verificar_postulacion_con_practica()
RETURNS TRIGGER AS $$
DECLARE
    v_pid    BIGINT;
    v_estado VARCHAR(20);
BEGIN
    IF TG_TABLE_NAME = 'postulacion' THEN v_pid := NEW.id; ELSE v_pid := OLD.postulacion_id; END IF;
    IF v_pid IS NULL THEN RETURN NULL; END IF;

    SELECT estado INTO v_estado FROM postulacion WHERE id = v_pid;
    IF v_estado = 'ACEPTADA' THEN
        IF NOT EXISTS (SELECT 1 FROM practica WHERE postulacion_id = v_pid) THEN
            RAISE EXCEPTION
                'La postulación % está ACEPTADA pero no tiene práctica: créela en la misma transacción', v_pid;
        END IF;
        IF EXISTS (SELECT 1 FROM practica
                    WHERE postulacion_id = v_pid AND estado IN ('CANCELADA','RECHAZADA')) THEN
            RAISE EXCEPTION
                'La postulación % sigue ACEPTADA con su práctica cancelada/rechazada: cancélela en la misma operación', v_pid;
        END IF;
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_postulacion_requiere_practica
    AFTER INSERT OR UPDATE ON postulacion
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION verificar_postulacion_con_practica();

CREATE CONSTRAINT TRIGGER trg_practica_mantiene_postulacion
    AFTER DELETE OR UPDATE OF postulacion_id ON practica
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION verificar_postulacion_con_practica();

-- 7.13 practica: consistencia con la postulación/oferta de origen y vínculo protegido
CREATE OR REPLACE FUNCTION validar_practica_postulacion()
RETURNS TRIGGER AS $$
DECLARE
    v_estado   VARCHAR(20);
    v_est      BIGINT;
    v_empresa  BIGINT;
    v_convenio BIGINT;
    v_tipo     BIGINT;
    v_periodo  BIGINT;
    v_horas_snapshot NUMERIC(6,2);
    v_nota_snapshot  NUMERIC(4,2);
    v_cambio_vinculo BOOLEAN := TRUE;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        v_cambio_vinculo := NEW.postulacion_id IS DISTINCT FROM OLD.postulacion_id;
        IF OLD.postulacion_id IS NULL AND NEW.postulacion_id IS NOT NULL THEN
            RAISE EXCEPTION 'Una práctica existente no puede convertirse en derivada de una postulación; créela en la transacción de aceptación';
        END IF;
        IF OLD.postulacion_id IS NOT NULL AND v_cambio_vinculo THEN
            IF NEW.postulacion_id IS NOT NULL THEN
                RAISE EXCEPTION 'No se permite sustituir una postulación por otra en una práctica existente';
            END IF;
            IF COALESCE(current_setting('praxis.desvincular_postulacion', TRUE), '') <> 'true'
               OR praxis_actor() IS NULL
               OR NOT (tiene_rol_activo(praxis_actor(), 'COORDINADOR')
                       OR tiene_rol_activo(praxis_actor(), 'ADMINISTRADOR'))
               OR NULLIF(btrim(current_setting('praxis.motivo', TRUE)), '') IS NULL THEN
                RAISE EXCEPTION
                    'Desvincular exige coordinador/administrador activo, contexto explícito y motivo';
            END IF;
            INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle)
            VALUES (praxis_actor(), 'PRACTICA_DESVINCULA_POSTULACION', 'practica', OLD.id,
                    jsonb_build_object('postulacion_id', OLD.postulacion_id,
                                       'motivo', current_setting('praxis.motivo', TRUE)));
        END IF;
    END IF;

    IF NEW.postulacion_id IS NULL THEN RETURN NEW; END IF;

    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'APROBADA' OR NEW.fecha_aprobacion IS NULL THEN
            RAISE EXCEPTION
                'Al aceptar académicamente la postulación, la práctica debe crearse en estado APROBADA y con fecha_aprobacion';
        END IF;
        IF NEW.creado_por IS DISTINCT FROM praxis_actor() THEN
            RAISE EXCEPTION 'La práctica derivada debe registrar como creador al coordinador autenticado';
        END IF;
    END IF;

    SELECT po.estado, po.estudiante_id, o.empresa_id, o.convenio_id,
           o.tipo_practica_id, o.periodo_id,
           o.horas_requeridas_snapshot, o.nota_minima_snapshot
      INTO v_estado, v_est, v_empresa, v_convenio, v_tipo, v_periodo,
           v_horas_snapshot, v_nota_snapshot
      FROM postulacion po
      JOIN oferta_perfil p   ON p.id = po.perfil_id
      JOIN oferta_practica o ON o.id = p.oferta_id
     WHERE po.id = NEW.postulacion_id;

    IF v_cambio_vinculo AND v_estado IS DISTINCT FROM 'ACEPTADA' THEN
        RAISE EXCEPTION
            'La postulación debe estar ACEPTADA para generar la práctica (actual: %)', v_estado;
    END IF;
    IF (NEW.estudiante_id, NEW.empresa_id, NEW.convenio_id, NEW.tipo_practica_id, NEW.periodo_id)
       IS DISTINCT FROM (v_est, v_empresa, v_convenio, v_tipo, v_periodo) THEN
        RAISE EXCEPTION 'La práctica no coincide con la oferta/postulación de origen';
    END IF;
    IF (NEW.horas_requeridas_snapshot, NEW.nota_minima_snapshot)
       IS DISTINCT FROM (v_horas_snapshot, v_nota_snapshot) THEN
        RAISE EXCEPTION 'Los snapshots académicos de la práctica deben coincidir con los publicados en la oferta';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_practica_postulacion
    BEFORE INSERT OR UPDATE OF postulacion_id, estudiante_id, empresa_id,
                               convenio_id, tipo_practica_id, periodo_id ON practica
    FOR EACH ROW EXECUTE FUNCTION validar_practica_postulacion();

-- 7.14 práctica cancelada/rechazada -> su postulación ACEPTADA pasa a CANCELADA (misma operación)
CREATE OR REPLACE FUNCTION sincronizar_postulacion_con_practica()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM set_config('praxis.sync_practica', 'true', TRUE);
    UPDATE postulacion
       SET estado = 'CANCELADA',
           motivo_decision = COALESCE(NULLIF(current_setting('praxis.motivo', TRUE), ''),
                                      'Práctica ' || lower(NEW.estado))
     WHERE id = NEW.postulacion_id AND estado = 'ACEPTADA';
    PERFORM set_config('praxis.sync_practica', '', TRUE);
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sincronizar_postulacion_practica
    AFTER UPDATE OF estado ON practica
    FOR EACH ROW WHEN (NEW.postulacion_id IS NOT NULL
                       AND NEW.estado IN ('CANCELADA','RECHAZADA')
                       AND OLD.estado IS DISTINCT FROM NEW.estado)
    EXECUTE FUNCTION sincronizar_postulacion_con_practica();

-- ============================================================
-- 8. PROCESO AUTOMÁTICO: cierre de ofertas vencidas
--    El backend lo invoca periódicamente: SELECT cerrar_ofertas_vencidas();
--    Queda identificado como operación del SISTEMA en el historial.
-- ============================================================

CREATE OR REPLACE FUNCTION cerrar_ofertas_vencidas()
RETURNS INT AS $$
DECLARE
    v_hoy DATE := (NOW() AT TIME ZONE 'America/Guayaquil')::date;
    v_n   INT;
BEGIN
    PERFORM set_config('praxis.usuario_id', '', TRUE);
    PERFORM set_config('praxis.sistema', 'true', TRUE);
    UPDATE oferta_practica
       SET estado = 'CERRADA',
           motivo_decision = 'Cierre automático por vencimiento del plazo de postulación'
     WHERE estado = 'PUBLICADA' AND fecha_cierre < v_hoy;
    GET DIAGNOSTICS v_n = ROW_COUNT;
    PERFORM set_config('praxis.sistema', '', TRUE);
    RETURN v_n;
END $$ LANGUAGE plpgsql;