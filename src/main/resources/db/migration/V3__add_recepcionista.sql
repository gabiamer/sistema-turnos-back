CREATE TABLE recepcionistas (
    id        BIGSERIAL PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL,
    apellido  VARCHAR(100) NOT NULL,
    email     VARCHAR(150) NOT NULL UNIQUE,
    rol       VARCHAR(20)  NOT NULL DEFAULT 'SECRETARIA'
);

ALTER TABLE turnos
    ADD COLUMN agendado_por BIGINT REFERENCES recepcionistas(id);
