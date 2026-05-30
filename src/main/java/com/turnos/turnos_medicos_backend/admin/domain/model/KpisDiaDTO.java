package com.turnos.turnos_medicos_backend.admin.domain.model;

public record KpisDiaDTO(
        long confirmados,
        long cancelados,
        long concluidos,
        long ausentes
) {}