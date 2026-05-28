-- Eliminar constraint viejo (los 5 valores originales)
ALTER TABLE turnos DROP CONSTRAINT IF EXISTS turnos_estado_check;

-- Recrear con los 6 valores actuales del enum EstadoTurno
ALTER TABLE turnos
    ADD CONSTRAINT turnos_estado_check
    CHECK (estado IN (
        'PENDIENTE',
        'CONFIRMADO',
        'CANCELADO',
        'BLOQUEADO',
        'EXPIRADO',
        'CONCLUIDA'
    ));
