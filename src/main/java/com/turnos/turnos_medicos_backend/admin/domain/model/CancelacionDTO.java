package com.turnos.turnos_medicos_backend.admin.domain.model;

import java.time.LocalDate;

public record CancelacionDTO(
        Long turnoId,
        LocalDate fecha,
        String especialidad,
        String pacienteAnonimizado,
        String motivo
) {}