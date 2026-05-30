package com.turnos.turnos_medicos_backend.admin.domain.model;

public record EspecialidadDTO(
        String especialidad,
        long totalTurnos
) {}