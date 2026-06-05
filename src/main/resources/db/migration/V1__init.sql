-- ── medicos ───────────────────────────────────────────────────────────────────
CREATE TABLE medicos (
    id           BIGSERIAL    PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    apellido     VARCHAR(100) NOT NULL,
    especialidad VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    creado_en    TIMESTAMP    DEFAULT NOW()
);

-- ── pacientes ─────────────────────────────────────────────────────────────────
CREATE TABLE pacientes (
    id               BIGSERIAL    PRIMARY KEY,
    ci               VARCHAR(20)  NOT NULL UNIQUE,
    nombre           VARCHAR(100) NOT NULL,
    apellido         VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE,
    telefono         VARCHAR(20),
    email            VARCHAR(150) NOT NULL UNIQUE,
    creado_en        TIMESTAMP    DEFAULT NOW()
);

-- ── turnos ────────────────────────────────────────────────────────────────────
-- (5 estados originales; V2 agrega CONCLUIDA)
CREATE TABLE turnos (
    id                 BIGSERIAL    PRIMARY KEY,
    paciente_id        BIGINT       NOT NULL REFERENCES pacientes(id),
    medico_id          BIGINT       NOT NULL REFERENCES medicos(id),
    fecha              DATE         NOT NULL,
    hora               TIME         NOT NULL,
    estado             VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    bloqueo_expira     TIMESTAMP,
    cancelado_por      VARCHAR(100),
    motivo_cancelacion VARCHAR(255),
    cancelado_en       TIMESTAMP,
    creado_en          TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT uq_paciente_fecha    UNIQUE (paciente_id, fecha),
    CONSTRAINT turnos_estado_check  CHECK  (estado IN (
        'PENDIENTE', 'CONFIRMADO', 'CANCELADO', 'BLOQUEADO', 'EXPIRADO'
    ))
);

-- ── agenda_medico ─────────────────────────────────────────────────────────────
CREATE TABLE agenda_medico (
    id               BIGSERIAL PRIMARY KEY,
    medico_id        BIGINT    NOT NULL REFERENCES medicos(id),
    dia_semana       INTEGER   NOT NULL,
    hora_inicio      TIME      NOT NULL,
    hora_fin         TIME      NOT NULL,
    duracion_minutos INTEGER   NOT NULL DEFAULT 30,
    activo           BOOLEAN   NOT NULL DEFAULT TRUE
);

-- ── bloqueo_dia ───────────────────────────────────────────────────────────────
CREATE TABLE bloqueo_dia (
    id           BIGSERIAL    PRIMARY KEY,
    medico_id    BIGINT       NOT NULL REFERENCES medicos(id),
    fecha_inicio DATE         NOT NULL,
    fecha_fin    DATE         NOT NULL,
    motivo       VARCHAR(255)
);
