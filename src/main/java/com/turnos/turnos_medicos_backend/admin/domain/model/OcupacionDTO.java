package com.turnos.turnos_medicos_backend.admin.domain.model;

public record OcupacionDTO(
        Long medicoId,
        String nombreMedico,
        String especialidad,
        long totalSlots,
        long ocupados,
        double porcentaje
) {}