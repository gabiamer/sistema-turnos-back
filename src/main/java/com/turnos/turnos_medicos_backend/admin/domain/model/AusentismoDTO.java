package com.turnos.turnos_medicos_backend.admin.domain.model;

import java.time.LocalDate;

public record AusentismoDTO(
        Long turnoId,
        LocalDate fecha,
        String nombreMedico,
        String especialidad
) {}