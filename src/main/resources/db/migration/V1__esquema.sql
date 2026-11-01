-- ============================================================
-- PRAXIS — V1__esquema.sql  (PostgreSQL 15+; probado en diseño para PG 17)
-- 26 tablas + 1 vista. Triggers al final.
-- Convenciones: snake_case singular; PK BIGINT IDENTITY; TIMESTAMPTZ para eventos.
-- Snapshots de practica: se fijan al postular y quedan congelados al salir de POSTULADA.
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 1. SEGURIDAD Y CATÁLOGOS
-- ============================================================

CREATE TABLE rol (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo      VARCHAR(30) NOT NULL UNIQUE,
    nombre      VARCHAR(60) NOT NULL,
    descripcion TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rol_codigo CHECK (codigo IN (
        'ESTUDIANTE','DOCENTE','TUTOR_EMPRESARIAL',
        'COORDINADOR','ADMINISTRADOR','AUTORIDAD'))
);

CREATE TABLE usuario (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    correo                VARCHAR(120) NOT NULL UNIQUE,
    hash_password         VARCHAR(72)  NOT NULL,
    nombres               VARCHAR(100) NOT NULL,
    apellidos             VARCHAR(100) NOT NULL,
    numero_documento      VARCHAR(20)  UNIQUE,
    tipo_documento        VARCHAR(20)  NOT NULL DEFAULT 'CEDULA',
    telefono              VARCHAR(20),
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    alcance_global        BOOLEAN      NOT NULL DEFAULT FALSE,
    debe_cambiar_password BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos_fallidos     INT          NOT NULL DEFAULT 0,
    bloqueado_hasta       TIMESTAMPTZ,
    ultimo_login          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_usuario_correo_lower CHECK (correo = lower(correo)),
    CONSTRAINT chk_usuario_correo       CHECK (
        correo ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$'),
    CONSTRAINT chk_usuario_tipo_doc     CHECK (tipo_documento IN ('CEDULA','PASAPORTE')),
    CONSTRAINT chk_usuario_documento    CHECK (
        numero_documento IS NULL
        OR (tipo_documento = 'CEDULA'    AND numero_documento ~ '^[0-9]{10}$')
        OR (tipo_documento = 'PASAPORTE' AND numero_documento ~ '^[A-Z0-9]{5,20}$')),
    CONSTRAINT chk_usuario_intentos     CHECK (intentos_fallidos >= 0)
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    rol_id     BIGINT      NOT NULL REFERENCES rol(id)     ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (usuario_id, rol_id)
);
CREATE INDEX idx_usuario_rol_rol ON usuario_rol(rol_id);

CREATE TABLE facultad (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo     VARCHAR(20)  NOT NULL UNIQUE,
    nombre     VARCHAR(150) NOT NULL UNIQUE,
    activa     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE carrera (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    facultad_id BIGINT       NOT NULL REFERENCES facultad(id) ON DELETE RESTRICT,
    codigo      VARCHAR(20)  NOT NULL UNIQUE,
    nombre      VARCHAR(150) NOT NULL,
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_carrera_facultad_nombre UNIQUE (facultad_id, nombre)
);
CREATE INDEX idx_carrera_facultad ON carrera(facultad_id);

CREATE TABLE usuario_carrera (
    usuario_id BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    carrera_id BIGINT      NOT NULL REFERENCES carrera(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (usuario_id, carrera_id)
);
CREATE INDEX idx_usuario_carrera_carrera ON usuario_carrera(carrera_id);

CREATE TABLE periodo_academico (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo       VARCHAR(20)  NOT NULL UNIQUE,
    nombre       VARCHAR(100) NOT NULL,
    fecha_inicio DATE         NOT NULL,
    fecha_fin    DATE         NOT NULL,
    activo       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_periodo_fechas CHECK (fecha_fin >= fecha_inicio)
);
CREATE UNIQUE INDEX uq_periodo_unico_activo
    ON periodo_academico((activo)) WHERE activo = TRUE;

CREATE TABLE estudiante (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id        BIGINT      NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE RESTRICT,
    carrera_id        BIGINT      NOT NULL REFERENCES carrera(id)        ON DELETE RESTRICT,
    codigo_estudiante VARCHAR(30) UNIQUE,
    fecha_nacimiento  DATE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_estudiante_carrera ON estudiante(carrera_id);

CREATE TABLE docente (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id     BIGINT      NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE RESTRICT,
    codigo_docente VARCHAR(20) UNIQUE,
    dedicacion     VARCHAR(50),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE empresa (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruc              VARCHAR(13)  NOT NULL UNIQUE,
    razon_social     VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    direccion        VARCHAR(250),
    telefono         VARCHAR(20),
    correo           VARCHAR(120),
    activa           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_empresa_ruc CHECK (ruc ~ '^[0-9]{13}$')
);

-- Decisión: un tutor pertenece a una sola empresa (usuario_id UNIQUE)
CREATE TABLE tutor_empresarial (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT      NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE RESTRICT,
    empresa_id BIGINT      NOT NULL REFERENCES empresa(id)        ON DELETE RESTRICT,
    cargo      VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tutor_empresa UNIQUE (id, empresa_id)   -- para FK compuesta desde practica
);
CREATE INDEX idx_tutor_empresa ON tutor_empresarial(empresa_id);

-- ============================================================
-- 2. REQUISITOS
-- ============================================================

CREATE TABLE requisito (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT,
    obligatorio BOOLEAN      NOT NULL DEFAULT TRUE,
    carrera_id  BIGINT       REFERENCES carrera(id) ON DELETE RESTRICT,  -- NULL = global
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_requisito_nombre_carrera
        UNIQUE NULLS NOT DISTINCT (nombre, carrera_id)
);
CREATE INDEX idx_requisito_carrera ON requisito(carrera_id) WHERE carrera_id IS NOT NULL;

CREATE TABLE requisito_estudiante (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estudiante_id      BIGINT      NOT NULL REFERENCES estudiante(id)        ON DELETE RESTRICT,
    requisito_id       BIGINT      NOT NULL REFERENCES requisito(id)         ON DELETE RESTRICT,
    periodo_id         BIGINT      NOT NULL REFERENCES periodo_academico(id) ON DELETE RESTRICT,
    estado             VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_verificacion TIMESTAMPTZ,
    verificado_por     BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    observacion        TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_req_estudiante_periodo UNIQUE (estudiante_id, requisito_id, periodo_id),
    CONSTRAINT chk_req_estado CHECK (estado IN ('PENDIENTE','CUMPLIDO','RECHAZADO')),
    CONSTRAINT chk_req_verificacion CHECK (
        (estado = 'PENDIENTE' AND fecha_verificacion IS NULL AND verificado_por IS NULL)
        OR (estado IN ('CUMPLIDO','RECHAZADO')
            AND fecha_verificacion IS NOT NULL AND verificado_por IS NOT NULL))
);
CREATE INDEX idx_req_estudiante_estado  ON requisito_estudiante(estudiante_id, estado);
CREATE INDEX idx_req_estudiante_req     ON requisito_estudiante(requisito_id);
CREATE INDEX idx_req_estudiante_periodo ON requisito_estudiante(periodo_id);

-- ============================================================
-- 3. PROCESO CENTRAL
-- ============================================================

CREATE TABLE convenio (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id    BIGINT      NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT,
    numero        VARCHAR(50) UNIQUE,
    fecha_inicio  DATE        NOT NULL,
    fecha_fin     DATE        NOT NULL,
    estado        VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',
    documento_url VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_convenio_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT chk_convenio_estado CHECK (estado IN ('VIGENTE','SUSPENDIDO','TERMINADO')),
    CONSTRAINT uq_convenio_empresa UNIQUE (id, empresa_id)   -- para FK compuesta desde practica
);
CREATE INDEX idx_convenio_empresa_estado ON convenio(empresa_id, estado);

-- VENCIDO se deriva aquí (zona Ecuador); los servicios leen estado_efectivo
CREATE OR REPLACE VIEW v_convenio_estado AS
SELECT c.id, c.empresa_id, c.numero, c.fecha_inicio, c.fecha_fin, c.estado,
       CASE
         WHEN c.estado <> 'VIGENTE' THEN c.estado
         WHEN c.fecha_fin < (NOW() AT TIME ZONE 'America/Guayaquil')::date THEN 'VENCIDO'
         ELSE 'VIGENTE'
       END AS estado_efectivo
  FROM convenio c;

CREATE TABLE tipo_practica (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre           VARCHAR(80)  NOT NULL UNIQUE,
    horas_requeridas NUMERIC(6,2) NOT NULL,
    nota_minima      NUMERIC(4,2) NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tipo_practica_horas CHECK (horas_requeridas > 0),
    CONSTRAINT chk_tipo_practica_nota  CHECK (nota_minima BETWEEN 0 AND 20)
);

CREATE TABLE practica (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estudiante_id             BIGINT       NOT NULL REFERENCES estudiante(id)        ON DELETE RESTRICT,
    carrera_id                BIGINT       NOT NULL REFERENCES carrera(id)           ON DELETE RESTRICT,
    empresa_id                BIGINT       NOT NULL REFERENCES empresa(id)           ON DELETE RESTRICT,
    convenio_id               BIGINT       NOT NULL,
    tipo_practica_id          BIGINT       NOT NULL REFERENCES tipo_practica(id)     ON DELETE RESTRICT,
    periodo_id                BIGINT       NOT NULL REFERENCES periodo_academico(id) ON DELETE RESTRICT,
    docente_id                BIGINT       REFERENCES docente(id) ON DELETE RESTRICT,
    tutor_empresarial_id      BIGINT,
    estado                    VARCHAR(30)  NOT NULL DEFAULT 'POSTULADA',
    horas_requeridas_snapshot NUMERIC(6,2) NOT NULL,
    nota_minima_snapshot      NUMERIC(4,2) NOT NULL,
    fecha_postulacion         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_aprobacion          TIMESTAMPTZ,
    fecha_inicio              DATE,
    fecha_fin                 DATE,
    nota_final                NUMERIC(4,2),
    observaciones             TEXT,
    creado_por                BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    version                   BIGINT       NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_practica_convenio_empresa
        FOREIGN KEY (convenio_id, empresa_id)
        REFERENCES convenio(id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_practica_tutor_empresa
        FOREIGN KEY (tutor_empresarial_id, empresa_id)
        REFERENCES tutor_empresarial(id, empresa_id) ON DELETE RESTRICT,

    CONSTRAINT chk_practica_estado CHECK (estado IN (
        'POSTULADA','APROBADA','EN_CURSO','HORAS_COMPLETAS','EVALUADA',
        'FINALIZADA','REPROBADA','RECHAZADA','CANCELADA')),
    CONSTRAINT chk_practica_horas_snap CHECK (horas_requeridas_snapshot > 0),
    CONSTRAINT chk_practica_nota_snap  CHECK (nota_minima_snapshot BETWEEN 0 AND 20),
    CONSTRAINT chk_practica_nota       CHECK (nota_final IS NULL OR nota_final BETWEEN 0 AND 20),
    CONSTRAINT chk_practica_fechas     CHECK (
        (fecha_inicio IS NULL AND fecha_fin IS NULL)
        OR (fecha_inicio IS NOT NULL AND fecha_fin IS NOT NULL AND fecha_fin >= fecha_inicio)),
    CONSTRAINT chk_practica_aprob CHECK (
        fecha_aprobacion IS NULL OR fecha_aprobacion >= fecha_postulacion),
    CONSTRAINT chk_practica_tiene_aprob CHECK (
        estado IN ('POSTULADA','RECHAZADA','CANCELADA') OR fecha_aprobacion IS NOT NULL),
    CONSTRAINT chk_practica_tiene_asignaciones CHECK (
        estado NOT IN ('EN_CURSO','HORAS_COMPLETAS','EVALUADA','FINALIZADA','REPROBADA')
        OR (docente_id IS NOT NULL AND tutor_empresarial_id IS NOT NULL
            AND fecha_inicio IS NOT NULL AND fecha_fin IS NOT NULL)),
    CONSTRAINT chk_practica_tiene_nota CHECK (
        estado NOT IN ('EVALUADA','FINALIZADA','REPROBADA') OR nota_final IS NOT NULL),
    CONSTRAINT chk_practica_finalizada_aprob CHECK (
        estado <> 'FINALIZADA' OR nota_final >= nota_minima_snapshot),
    CONSTRAINT chk_practica_reprobada CHECK (
        estado <> 'REPROBADA' OR nota_final < nota_minima_snapshot)
);

CREATE INDEX idx_practica_estudiante_periodo ON practica(estudiante_id, periodo_id);
CREATE INDEX idx_practica_estado_periodo     ON practica(estado, periodo_id);
CREATE INDEX idx_practica_carrera_estado     ON practica(carrera_id, estado);
CREATE INDEX idx_practica_docente            ON practica(docente_id)           WHERE docente_id IS NOT NULL;
CREATE INDEX idx_practica_tutor              ON practica(tutor_empresarial_id) WHERE tutor_empresarial_id IS NOT NULL;
CREATE INDEX idx_practica_empresa            ON practica(empresa_id);
CREATE INDEX idx_practica_periodo            ON practica(periodo_id);
CREATE INDEX idx_practica_tipo               ON practica(tipo_practica_id);
CREATE INDEX idx_practica_convenio           ON practica(convenio_id, empresa_id);
CREATE INDEX idx_practica_creado_por         ON practica(creado_por);

-- Una sola práctica "viva" por estudiante y periodo
CREATE UNIQUE INDEX uq_practica_activa_por_periodo
    ON practica(estudiante_id, periodo_id)
    WHERE estado NOT IN ('RECHAZADA','CANCELADA','REPROBADA');

CREATE TABLE practica_estado_historial (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id     BIGINT      NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    estado_anterior VARCHAR(30),
    estado_nuevo    VARCHAR(30) NOT NULL,
    usuario_id      BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    motivo          TEXT,
    fecha           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_hist_estado_anterior CHECK (
        estado_anterior IS NULL OR estado_anterior IN (
            'POSTULADA','APROBADA','EN_CURSO','HORAS_COMPLETAS','EVALUADA',
            'FINALIZADA','REPROBADA','RECHAZADA','CANCELADA')),
    CONSTRAINT chk_hist_estado_nuevo CHECK (estado_nuevo IN (
        'POSTULADA','APROBADA','EN_CURSO','HORAS_COMPLETAS','EVALUADA',
        'FINALIZADA','REPROBADA','RECHAZADA','CANCELADA'))
);
CREATE INDEX idx_hist_practica_fecha ON practica_estado_historial(practica_id, fecha DESC);

CREATE TABLE actividad_practica (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id    BIGINT      NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    fecha          DATE        NOT NULL,
    descripcion    TEXT        NOT NULL,
    registrada_por BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_actividad_practica ON actividad_practica(practica_id, fecha);

CREATE TABLE registro_horas (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id      BIGINT       NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    fecha            DATE         NOT NULL,
    horas            NUMERIC(4,2) NOT NULL,
    descripcion      TEXT,
    validado         BOOLEAN      NOT NULL DEFAULT FALSE,
    validado_por     BIGINT       REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_validacion TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_registro_horas_dia    UNIQUE (practica_id, fecha),
    CONSTRAINT chk_registro_horas_rango CHECK (horas > 0 AND horas <= 8),
    CONSTRAINT chk_registro_validacion  CHECK (
        (validado = FALSE AND validado_por IS NULL AND fecha_validacion IS NULL)
        OR (validado = TRUE AND validado_por IS NOT NULL AND fecha_validacion IS NOT NULL))
);

CREATE TABLE informe (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id    BIGINT       NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    tipo           VARCHAR(20)  NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'ENTREGADO',
    nombre_archivo VARCHAR(255) NOT NULL,
    nombre_interno VARCHAR(255) NOT NULL UNIQUE,
    content_type   VARCHAR(60)  NOT NULL DEFAULT 'application/pdf',
    subido_por     BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_subida   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    observaciones  TEXT,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_informe_tipo   CHECK (tipo   IN ('PARCIAL','FINAL')),
    CONSTRAINT chk_informe_estado CHECK (estado IN ('ENTREGADO','OBSERVADO','APROBADO')),
    CONSTRAINT chk_informe_pdf    CHECK (content_type = 'application/pdf')
);
CREATE INDEX idx_informe_practica_estado ON informe(practica_id, estado);
CREATE INDEX idx_informe_subido_por      ON informe(subido_por);
CREATE UNIQUE INDEX uq_informe_final_aprobado
    ON informe(practica_id) WHERE tipo = 'FINAL' AND estado = 'APROBADO';

CREATE TABLE informe_archivo (
    informe_id   BIGINT      PRIMARY KEY REFERENCES informe(id) ON DELETE CASCADE,
    contenido    BYTEA       NOT NULL,
    tamano_bytes INT         NOT NULL,
    sha256       VARCHAR(64) NOT NULL,
    CONSTRAINT chk_archivo_tamano      CHECK (tamano_bytes > 0 AND tamano_bytes <= 2097152),
    CONSTRAINT chk_archivo_tamano_real CHECK (tamano_bytes = octet_length(contenido)),
    CONSTRAINT chk_archivo_sha256      CHECK (sha256 ~ '^[0-9a-f]{64}$')
);

-- ============================================================
-- 4. SEGUIMIENTO, REVISIÓN Y AUDITORÍA
-- ============================================================

CREATE TABLE evaluacion (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id          BIGINT       NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    evaluador_usuario_id BIGINT       NOT NULL REFERENCES usuario(id)  ON DELETE RESTRICT,
    rol_evaluador        VARCHAR(30)  NOT NULL,
    criterios            JSONB,
    nota                 NUMERIC(4,2) NOT NULL,
    observacion          TEXT,
    fecha_evaluacion     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_eval_practica_rol UNIQUE (practica_id, rol_evaluador),
    CONSTRAINT chk_eval_rol  CHECK (rol_evaluador IN ('DOCENTE','TUTOR_EMPRESARIAL')),
    CONSTRAINT chk_eval_nota CHECK (nota BETWEEN 0 AND 20)
);
CREATE INDEX idx_eval_evaluador ON evaluacion(evaluador_usuario_id);

CREATE TABLE visita_seguimiento (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id   BIGINT      NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    docente_id    BIGINT      NOT NULL REFERENCES docente(id)  ON DELETE RESTRICT,
    fecha         DATE        NOT NULL,
    observaciones TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_visita_practica_fecha ON visita_seguimiento(practica_id, fecha);
CREATE INDEX idx_visita_docente        ON visita_seguimiento(docente_id);

CREATE TABLE incidencia (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id      BIGINT      NOT NULL REFERENCES practica(id) ON DELETE CASCADE,
    reportada_por    BIGINT      NOT NULL REFERENCES usuario(id)  ON DELETE RESTRICT,
    descripcion      TEXT        NOT NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
    resuelta_por     BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_resolucion TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_incidencia_estado CHECK (estado IN ('ABIERTA','RESUELTA')),
    CONSTRAINT chk_incidencia_resolucion CHECK (
        (estado = 'ABIERTA'  AND fecha_resolucion IS NULL AND resuelta_por IS NULL)
        OR (estado = 'RESUELTA' AND fecha_resolucion IS NOT NULL AND resuelta_por IS NOT NULL))
);
CREATE INDEX idx_incidencia_practica_estado ON incidencia(practica_id, estado);

CREATE TABLE certificado (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    practica_id         BIGINT      NOT NULL REFERENCES practica(id) ON DELETE RESTRICT,
    codigo_verificacion UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    datos_snapshot      JSONB       NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',
    fecha_emision       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    emitido_por         BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    motivo_anulacion    TEXT,
    fecha_anulacion     TIMESTAMPTZ,
    anulado_por         BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_certificado_estado    CHECK (estado IN ('VIGENTE','ANULADO')),
    CONSTRAINT chk_certificado_snapshot  CHECK (jsonb_typeof(datos_snapshot) = 'object'),
    CONSTRAINT chk_certificado_anulacion CHECK (
        (estado = 'VIGENTE' AND fecha_anulacion IS NULL
                            AND anulado_por IS NULL AND motivo_anulacion IS NULL)
        OR (estado = 'ANULADO' AND fecha_anulacion IS NOT NULL
                               AND anulado_por IS NOT NULL AND motivo_anulacion IS NOT NULL))
);
CREATE UNIQUE INDEX uq_certificado_vigente_por_practica
    ON certificado(practica_id) WHERE estado = 'VIGENTE';

CREATE TABLE auditoria (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT      REFERENCES usuario(id) ON DELETE RESTRICT,
    accion     VARCHAR(50) NOT NULL,
    entidad    VARCHAR(50) NOT NULL,
    entidad_id BIGINT,
    detalle    JSONB,
    fecha      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_auditoria_usuario_fecha ON auditoria(usuario_id, fecha DESC);
CREATE INDEX idx_auditoria_entidad       ON auditoria(entidad, entidad_id);

-- ============================================================
-- 5. TRIGGERS
-- ============================================================

-- 5.1 updated_at automático
DO $$
DECLARE t TEXT;
BEGIN
    FOR t IN
        SELECT tablename FROM pg_tables
        WHERE schemaname = current_schema()
          AND tablename IN (
            'rol','usuario','facultad','carrera','periodo_academico',
            'estudiante','docente','empresa','tutor_empresarial',
            'requisito','requisito_estudiante','convenio','tipo_practica',
            'practica','actividad_practica','registro_horas','informe',
            'evaluacion','visita_seguimiento','incidencia')
    LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%I_updated BEFORE UPDATE ON %I
             FOR EACH ROW EXECUTE FUNCTION set_updated_at()', t, t);
    END LOOP;
END $$;

-- 5.2 registro_horas: fecha válida (zona Ecuador), dentro de rango y práctica EN_CURSO
--     (aplica también al validar, que es un UPDATE: intencional)
CREATE OR REPLACE FUNCTION validar_registro_horas()
RETURNS TRIGGER AS $$
DECLARE
    v_ini DATE; v_fin DATE; v_estado VARCHAR(30);
    v_hoy DATE := (NOW() AT TIME ZONE 'America/Guayaquil')::date;
BEGIN
    IF NEW.fecha > v_hoy THEN
        RAISE EXCEPTION 'registro_horas.fecha no puede ser futura (hoy=%)', v_hoy;
    END IF;
    SELECT fecha_inicio, fecha_fin, estado INTO v_ini, v_fin, v_estado
      FROM practica WHERE id = NEW.practica_id;
    IF v_estado IS DISTINCT FROM 'EN_CURSO' THEN
        RAISE EXCEPTION 'Solo se registran horas en prácticas EN_CURSO (actual: %)', v_estado;
    END IF;
    IF v_ini IS NOT NULL AND NEW.fecha < v_ini THEN
        RAISE EXCEPTION 'registro_horas.fecha anterior al inicio de la práctica';
    END IF;
    IF v_fin IS NOT NULL AND NEW.fecha > v_fin THEN
        RAISE EXCEPTION 'registro_horas.fecha posterior al fin de la práctica';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_registro_horas
    BEFORE INSERT OR UPDATE ON registro_horas
    FOR EACH ROW EXECUTE FUNCTION validar_registro_horas();

-- 5.3 registro_horas: validados inmutables (UPDATE y DELETE)
CREATE OR REPLACE FUNCTION bloquear_horas_validadas()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.validado THEN
            RAISE EXCEPTION 'No se puede borrar un registro de horas validado';
        END IF;
        RETURN OLD;
    END IF;
    IF OLD.validado AND
       (OLD.practica_id, OLD.fecha, OLD.horas, OLD.descripcion,
        OLD.validado, OLD.validado_por, OLD.fecha_validacion)
       IS DISTINCT FROM
       (NEW.practica_id, NEW.fecha, NEW.horas, NEW.descripcion,
        NEW.validado, NEW.validado_por, NEW.fecha_validacion) THEN
        RAISE EXCEPTION 'Registro de horas validado: no modificable';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bloquear_horas_validadas
    BEFORE UPDATE OR DELETE ON registro_horas
    FOR EACH ROW EXECUTE FUNCTION bloquear_horas_validadas();

-- 5.4 practica: carrera_id debe coincidir con la del estudiante al insertar
CREATE OR REPLACE FUNCTION validar_practica_insert()
RETURNS TRIGGER AS $$
DECLARE v_carrera BIGINT;
BEGIN
    SELECT carrera_id INTO v_carrera FROM estudiante WHERE id = NEW.estudiante_id;
    IF NEW.carrera_id IS DISTINCT FROM v_carrera THEN
        RAISE EXCEPTION
            'practica.carrera_id debe coincidir con la carrera actual del estudiante (estudiante=%, esperada=%, recibida=%)',
            NEW.estudiante_id, v_carrera, NEW.carrera_id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_practica_insert
    BEFORE INSERT ON practica
    FOR EACH ROW EXECUTE FUNCTION validar_practica_insert();

-- 5.5 practica: no borrar prácticas alguna vez aprobadas
CREATE OR REPLACE FUNCTION proteger_borrado_practica()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.fecha_aprobacion IS NOT NULL THEN
        RAISE EXCEPTION 'No se puede borrar una práctica que fue aprobada; use CANCELADA';
    END IF;
    RETURN OLD;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_borrado_practica
    BEFORE DELETE ON practica
    FOR EACH ROW EXECUTE FUNCTION proteger_borrado_practica();

-- 5.6 practica: snapshots/llaves fijados al postular, congelados al salir de POSTULADA
CREATE OR REPLACE FUNCTION proteger_snapshots_practica()
RETURNS TRIGGER AS $$
BEGIN
    IF (OLD.horas_requeridas_snapshot, OLD.nota_minima_snapshot,
        OLD.tipo_practica_id, OLD.estudiante_id, OLD.carrera_id,
        OLD.empresa_id, OLD.periodo_id, OLD.creado_por)
       IS DISTINCT FROM
       (NEW.horas_requeridas_snapshot, NEW.nota_minima_snapshot,
        NEW.tipo_practica_id, NEW.estudiante_id, NEW.carrera_id,
        NEW.empresa_id, NEW.periodo_id, NEW.creado_por)
       AND (OLD.estado <> 'POSTULADA' OR NEW.estado <> 'POSTULADA') THEN
        RAISE EXCEPTION 'Datos congelados de la práctica no modificables';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_snapshots
    BEFORE UPDATE ON practica
    FOR EACH ROW EXECUTE FUNCTION proteger_snapshots_practica();

-- 5.7 practica: máquina de estados
CREATE OR REPLACE FUNCTION validar_transicion_practica()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado = OLD.estado THEN RETURN NEW; END IF;
    IF NOT (
           (OLD.estado = 'POSTULADA'       AND NEW.estado IN ('APROBADA','RECHAZADA','CANCELADA'))
        OR (OLD.estado = 'APROBADA'        AND NEW.estado IN ('EN_CURSO','CANCELADA'))
        OR (OLD.estado = 'EN_CURSO'        AND NEW.estado IN ('HORAS_COMPLETAS','CANCELADA'))
        OR (OLD.estado = 'HORAS_COMPLETAS' AND NEW.estado IN ('EVALUADA','CANCELADA'))
        OR (OLD.estado = 'EVALUADA'        AND NEW.estado IN ('FINALIZADA','REPROBADA','CANCELADA'))
    ) THEN
        RAISE EXCEPTION 'Transición inválida: % -> %', OLD.estado, NEW.estado;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_transicion_practica
    BEFORE UPDATE OF estado ON practica
    FOR EACH ROW EXECUTE FUNCTION validar_transicion_practica();

-- 5.8 practica: historial automático (estado inicial + cambios).
--     Usuario y motivo vía set_config('praxis.usuario_id' / 'praxis.motivo', valor, true)
CREATE OR REPLACE FUNCTION registrar_historial_practica()
RETURNS TRIGGER AS $$
DECLARE
    v_usuario BIGINT := NULLIF(current_setting('praxis.usuario_id', TRUE), '')::BIGINT;
    v_motivo  TEXT   := NULLIF(current_setting('praxis.motivo', TRUE), '');
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO practica_estado_historial
            (practica_id, estado_anterior, estado_nuevo, usuario_id, motivo)
        VALUES (NEW.id, NULL, NEW.estado, v_usuario, v_motivo);
    ELSIF NEW.estado IS DISTINCT FROM OLD.estado THEN
        INSERT INTO practica_estado_historial
            (practica_id, estado_anterior, estado_nuevo, usuario_id, motivo)
        VALUES (NEW.id, OLD.estado, NEW.estado, v_usuario, v_motivo);
    END IF;
    RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_registrar_historial_practica
    AFTER INSERT OR UPDATE OF estado ON practica
    FOR EACH ROW EXECUTE FUNCTION registrar_historial_practica();

-- 5.9 historial: no editable (DELETE queda permitido para el CASCADE de prácticas nunca aprobadas)
CREATE OR REPLACE FUNCTION proteger_historial_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'practica_estado_historial es inmutable';
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_historial_update
    BEFORE UPDATE ON practica_estado_historial
    FOR EACH ROW EXECUTE FUNCTION proteger_historial_update();

-- 5.10 auditoria inmutable
CREATE OR REPLACE FUNCTION impedir_modificacion_auditoria()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'auditoria es inmutable';
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditoria_inmutable
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_auditoria();

-- 5.11 certificado: solo para FINALIZADA, inmutable, ANULADO definitivo, no se borra
CREATE OR REPLACE FUNCTION proteger_certificado()
RETURNS TRIGGER AS $$
DECLARE v_estado VARCHAR(30);
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Los certificados no se borran; anúlelos';
    END IF;

    IF TG_OP = 'INSERT' THEN
        SELECT estado INTO v_estado FROM practica WHERE id = NEW.practica_id;
        IF v_estado IS DISTINCT FROM 'FINALIZADA' THEN
            RAISE EXCEPTION
                'Certificado solo se emite para prácticas FINALIZADA (actual: %)', v_estado;
        END IF;
        RETURN NEW;
    END IF;

    IF OLD.estado = 'ANULADO' THEN
        RAISE EXCEPTION 'Un certificado ANULADO es definitivo';
    END IF;

    IF (OLD.datos_snapshot, OLD.codigo_verificacion, OLD.practica_id,
        OLD.fecha_emision, OLD.emitido_por)
       IS DISTINCT FROM
       (NEW.datos_snapshot, NEW.codigo_verificacion, NEW.practica_id,
        NEW.fecha_emision, NEW.emitido_por) THEN
        RAISE EXCEPTION
            'Certificado: datos_snapshot, código, práctica, emisión y emisor son inmutables';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_certificado
    BEFORE INSERT OR UPDATE OR DELETE ON certificado
    FOR EACH ROW EXECUTE FUNCTION proteger_certificado();

-- 5.12 informe APROBADO: inmutable y no borrable
CREATE OR REPLACE FUNCTION proteger_informe()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.estado = 'APROBADO' THEN
            RAISE EXCEPTION 'Informe APROBADO: no se puede borrar';
        END IF;
        RETURN OLD;
    END IF;

    IF OLD.estado = 'APROBADO' AND
       (OLD.practica_id, OLD.tipo, OLD.estado, OLD.nombre_archivo, OLD.nombre_interno,
        OLD.content_type, OLD.subido_por, OLD.fecha_subida)
       IS DISTINCT FROM
       (NEW.practica_id, NEW.tipo, NEW.estado, NEW.nombre_archivo, NEW.nombre_interno,
        NEW.content_type, NEW.subido_por, NEW.fecha_subida) THEN
        RAISE EXCEPTION 'Informe APROBADO: inmutable';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_informe
    BEFORE UPDATE OR DELETE ON informe
    FOR EACH ROW EXECUTE FUNCTION proteger_informe();

-- 5.13 informe_archivo: no modificar/borrar si el informe está APROBADO
CREATE OR REPLACE FUNCTION proteger_informe_archivo()
RETURNS TRIGGER AS $$
DECLARE v_estado VARCHAR(20);
BEGIN
    SELECT estado INTO v_estado FROM informe WHERE id = OLD.informe_id;
    IF v_estado = 'APROBADO' THEN
        RAISE EXCEPTION 'Informe APROBADO: archivo inmutable';
    END IF;
    RETURN CASE TG_OP WHEN 'DELETE' THEN OLD ELSE NEW END;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_informe_archivo
    BEFORE UPDATE OR DELETE ON informe_archivo
    FOR EACH ROW EXECUTE FUNCTION proteger_informe_archivo();

-- 5.14 evaluacion: solo en EN_CURSO / HORAS_COMPLETAS (lista blanca).
--      Si la práctica ya no existe (borrado en cascada de una práctica nunca aprobada),
--      v_estado es NULL y se permite.
CREATE OR REPLACE FUNCTION proteger_evaluacion()
RETURNS TRIGGER AS $$
DECLARE
    v_estado      VARCHAR(30);
    v_practica_id BIGINT;
BEGIN
    v_practica_id := CASE TG_OP WHEN 'DELETE' THEN OLD.practica_id ELSE NEW.practica_id END;
    SELECT estado INTO v_estado FROM practica WHERE id = v_practica_id;
    IF v_estado IS NOT NULL AND v_estado NOT IN ('EN_CURSO','HORAS_COMPLETAS') THEN
        RAISE EXCEPTION
            'Evaluaciones solo en EN_CURSO/HORAS_COMPLETAS (actual: %)', v_estado;
    END IF;
    RETURN CASE TG_OP WHEN 'DELETE' THEN OLD ELSE NEW END;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_evaluacion
    BEFORE INSERT OR UPDATE OR DELETE ON evaluacion
    FOR EACH ROW EXECUTE FUNCTION proteger_evaluacion();
