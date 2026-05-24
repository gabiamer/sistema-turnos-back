package com.turnos.turnos_medicos_backend.turno.domain.port;

import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository {

    // Guarda o actualiza un turno
    Turno save(Turno turno);

    // Busca por ID (para confirmar o cancelar)
    Optional<Turno> findById(Long id);

    // Todos los turnos de un paciente (para "Mis Turnos")
    List<Turno> findByPacienteId(Long pacienteId);

    // Turnos de un médico en una fecha (para verificar disponibilidad)
    List<Turno> findByMedicoIdAndFecha(Long medicoId, LocalDate fecha);

    // Turnos bloqueados que ya expiraron (para el job de limpieza)
    List<Turno> findByEstadoAndBloqueoExpirasBefore(EstadoTurno estado, LocalDateTime ahora);
}