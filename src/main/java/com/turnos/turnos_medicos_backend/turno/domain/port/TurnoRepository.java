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
}