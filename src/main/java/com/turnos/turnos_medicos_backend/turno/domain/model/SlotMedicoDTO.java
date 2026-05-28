package com.turnos.turnos_medicos_backend.turno.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO enriquecido para la vista del médico.
 * Extiende SlotDTO añadiendo los datos del turno y paciente cuando el slot está ocupado.
 *
 * Contrato con el front:
 * {
 *   fecha, hora, disponible, bloqueado,
 *   turno?: { id, estado, paciente: { id, nombre, apellido, ci } }
 * }
 */
public record SlotMedicoDTO(
        LocalDate fecha,
        LocalTime hora,
        boolean disponible,
        boolean bloqueado,
        TurnoResumen turno       // null cuando el slot está libre o bloqueado
) {

    /**
     * Resumen del turno incrustado en el slot.
     * Solo los campos que la vista del médico necesita mostrar.
     */
    public record TurnoResumen(
            Long id,
            String estado,
            PacienteResumen paciente
    ) {}

    public record PacienteResumen(
            Long id,
            String nombre,
            String apellido,
            String ci
    ) {}

    /** Factory: slot libre o bloqueado (sin turno) */
    public static SlotMedicoDTO libre(LocalDate fecha, LocalTime hora, boolean bloqueado) {
        return new SlotMedicoDTO(fecha, hora, !bloqueado, bloqueado, null);
    }

    /** Factory: slot ocupado con datos del turno */
    public static SlotMedicoDTO ocupado(LocalDate fecha, LocalTime hora, TurnoResumen turno) {
        return new SlotMedicoDTO(fecha, hora, false, false, turno);
    }
}