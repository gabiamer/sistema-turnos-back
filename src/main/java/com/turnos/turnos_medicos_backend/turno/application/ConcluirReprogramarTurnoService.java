package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class ConcluirReprogramarTurnoService {

    private final TurnoRepository turnoRepository;
    private final DisponibilidadPort disponibilidadPort;

    public ConcluirReprogramarTurnoService(TurnoRepository turnoRepository,
                                            DisponibilidadPort disponibilidadPort) {
        this.turnoRepository = turnoRepository;
        this.disponibilidadPort = disponibilidadPort;
    }

    /**
     * Marca un turno como CONCLUIDA.
     * FIX: acepta tanto CONFIRMADO como PENDIENTE (un turno puede estar PENDIENTE
     * si el paciente lo reservó pero el sistema no lo confirmó explícitamente).
     * Requiere migración SQL para ampliar el check constraint en PostgreSQL
     * (ver fix_estado_constraint.sql).
     */
    public Turno cambiarEstado(Long turnoId, EstadoTurno nuevoEstado) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new TurnoNoEncontradoException("Turno no encontrado: " + turnoId));

        if (nuevoEstado != EstadoTurno.CONCLUIDA) {
            throw new EstadoInvalidoException("Solo se acepta el estado CONCLUIDA");
        }

        // FIX: aceptar CONFIRMADO y PENDIENTE — en ambos casos el médico puede concluir
        if (turno.getEstado() != EstadoTurno.CONFIRMADO && turno.getEstado() != EstadoTurno.PENDIENTE) {
            throw new EstadoInvalidoException(
                    "El turno debe estar en CONFIRMADO o PENDIENTE para poder concluirse (estado actual: "
                    + turno.getEstado() + ")");
        }

        turno.setEstado(EstadoTurno.CONCLUIDA);
        return turnoRepository.save(turno);
    }

    /**
     * Reprograma un turno a una nueva fecha y hora.
     *
     * FIX (uq_paciente_fecha): antes de actualizar la fecha, verifica que el
     * paciente no tenga ya otro turno activo en la nueva fecha. Sin esta
     * verificación, Hibernate lanza DataIntegrityViolationException al hacer
     * el UPDATE porque el constraint unique(paciente_id, fecha) se evalúa
     * antes de que el registro viejo "libere" esa combinación.
     */
    public Turno reprogramar(Long turnoId, LocalDate nuevaFecha, LocalTime nuevaHora) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new TurnoNoEncontradoException("Turno no encontrado: " + turnoId));

        if (turno.getEstado() == EstadoTurno.CONCLUIDA || turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new EstadoInvalidoException(
                    "No se puede reprogramar un turno con estado " + turno.getEstado());
        }

        // FIX: verificar que el slot de disponibilidad esté libre
        if (!disponibilidadPort.estaDisponible(turno.getMedico().getId(), nuevaFecha, nuevaHora)) {
            throw new SlotOcupadoException("El slot solicitado no está disponible");
        }

        // FIX: verificar que el paciente no tenga ya otro turno activo en la nueva fecha
        // (distinto al que se está reprogramando, para no bloquearse a sí mismo)
        boolean pacienteTieneConflicto = turnoRepository
                .findByPacienteId(turno.getPaciente().getId())
                .stream()
                .anyMatch(t -> !t.getId().equals(turnoId)
                        && t.getFecha().equals(nuevaFecha)
                        && t.getEstado() != EstadoTurno.CANCELADO
                        && t.getEstado() != EstadoTurno.EXPIRADO);

        if (pacienteTieneConflicto) {
            throw new PacienteConTurnoEseDiaException(
                    "El paciente ya tiene un turno activo el " + nuevaFecha);
        }

        turno.setFecha(nuevaFecha);
        turno.setHora(nuevaHora);
        turno.setEstado(EstadoTurno.CONFIRMADO);
        return turnoRepository.save(turno);
    }

    // ── Excepciones de dominio ────────────────────────────────────────────────
    public static class TurnoNoEncontradoException extends RuntimeException {
        public TurnoNoEncontradoException(String msg) { super(msg); }
    }

    public static class EstadoInvalidoException extends RuntimeException {
        public EstadoInvalidoException(String msg) { super(msg); }
    }

    public static class SlotOcupadoException extends RuntimeException {
        public SlotOcupadoException(String msg) { super(msg); }
    }

    public static class PacienteConTurnoEseDiaException extends RuntimeException {
        public PacienteConTurnoEseDiaException(String msg) { super(msg); }
    }
}