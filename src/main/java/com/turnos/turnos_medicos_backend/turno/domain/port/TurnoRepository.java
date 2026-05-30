package com.turnos.turnos_medicos_backend.turno.domain.port;

import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository {

    Turno save(Turno turno);

    Optional<Turno> findById(Long id);

    List<Turno> findByPacienteId(Long pacienteId);

    List<Turno> findByMedicoIdAndFecha(Long medicoId, LocalDate fecha);

    List<Turno> findByEstadoAndBloqueoExpiraBefore(EstadoTurno estado, LocalDateTime ahora);

    /** Todos los turnos de un médico (para construir la vista médico) */
    List<Turno> findByMedicoId(Long medicoId);

    /** Todos los turnos de una fecha concreta (para la vista de hoy de secretaría) */
    List<Turno> findByFecha(LocalDate fecha);
    

    /** Turnos de un rango de fechas con un estado dado */
    List<Turno> findByFechaBetweenAndEstado(LocalDate inicio, LocalDate fin, EstadoTurno estado);

    /** Todos los turnos en un rango de fechas */
    List<Turno> findByFechaBetween(LocalDate inicio, LocalDate fin);

    /** Turnos de una fecha con un estado dado */
    List<Turno> findByFechaAndEstado(LocalDate fecha, EstadoTurno estado);
}