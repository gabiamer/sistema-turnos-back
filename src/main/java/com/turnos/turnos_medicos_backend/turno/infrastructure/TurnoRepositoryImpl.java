package com.turnos.turnos_medicos_backend.turno.infrastructure;

import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnoRepositoryImpl
        extends JpaRepository<Turno, Long>, TurnoRepository {

    List<Turno> findByPacienteId(Long pacienteId);

    List<Turno> findByMedicoIdAndFecha(Long medicoId, LocalDate fecha);

    List<Turno> findByEstadoAndBloqueoExpiraBefore(EstadoTurno estado, LocalDateTime ahora);

    /** Nuevo: todos los turnos de un médico, para enriquecer la vista médico */
    List<Turno> findByMedicoId(Long medicoId);
}