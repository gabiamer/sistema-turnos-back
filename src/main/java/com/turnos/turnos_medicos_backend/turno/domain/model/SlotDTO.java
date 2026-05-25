package com.turnos.turnos_medicos_backend.turno.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record SlotDTO(
        LocalDate fecha,
        LocalTime hora,
        boolean disponible,
        boolean bloqueado
) {}