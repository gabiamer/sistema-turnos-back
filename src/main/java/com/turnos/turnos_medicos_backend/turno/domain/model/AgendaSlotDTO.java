package com.turnos.turnos_medicos_backend.turno.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record AgendaSlotDTO(
        LocalDate fecha,
        LocalTime hora,
        boolean disponible,
        boolean bloqueado,
        TurnoInfo turno
) {
    public record TurnoInfo(Long id, String estado, PacienteInfo paciente) {}
    public record PacienteInfo(Long id, String nombre, String apellido, String ci) {}
}