package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.shared.ports.IEventPublisher;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CancelarTurnoService {

    private final TurnoRepository turnoRepository;
    private final IEventPublisher eventPublisher;

    public CancelarTurnoService(TurnoRepository turnoRepository,
                                 IEventPublisher eventPublisher) {
        this.turnoRepository = turnoRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Turno> listarPorPaciente(Long pacienteId) {
        return turnoRepository.findByPacienteId(pacienteId);
    }

    /**
     * CU-02: El PACIENTE cancela su propio turno.
     * Valida que el turno pertenezca al paciente y que falten más de 2h.
     */
    public Turno cancelarTurno(Long turnoId, Long pacienteId, String motivo) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        // Solo el paciente dueño puede usar este flujo
        if (!turno.getPaciente().getId().equals(pacienteId)) {
            throw new NoAutorizadoException("No autorizado");
        }

        LocalDateTime limiteCancelacion = LocalDateTime.of(turno.getFecha(), turno.getHora())
                .minusHours(2);
        if (LocalDateTime.now().isAfter(limiteCancelacion)) {
            throw new FueraDePlazoException("No se puede cancelar con menos de 2hs");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        turno.setCanceladoPor("paciente:" + pacienteId);
        turno.setMotivoCancelacion(motivo);
        turno.setCanceladoEn(LocalDateTime.now());

        Turno cancelado = turnoRepository.save(turno);
        eventPublisher.publicar("TURNO_CANCELADO", cancelado.getId());
        return cancelado;
    }

    /**
     * FIX — El MÉDICO cancela un turno de su agenda.
     * No requiere validación de ownership por paciente.
     * Sí valida que el turno no esté ya cancelado o concluido.
     * La restricción de 2h NO aplica cuando cancela el médico
     * (el médico puede tener una urgencia en cualquier momento).
     */
    public Turno cancelarTurnoMedico(Long turnoId, String motivo) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new YaCanceladoException("El turno ya está cancelado");
        }
        if (turno.getEstado() == EstadoTurno.CONCLUIDA) {
            throw new EstadoInvalidoException("No se puede cancelar un turno ya concluido");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        turno.setCanceladoPor("medico:" + turno.getMedico().getId());
        turno.setMotivoCancelacion(motivo);
        turno.setCanceladoEn(LocalDateTime.now());

        Turno cancelado = turnoRepository.save(turno);
        eventPublisher.publicar("TURNO_CANCELADO_POR_MEDICO", cancelado.getId());
        return cancelado;
    }

    // ── Excepciones de dominio ────────────────────────────────────────────────
    public static class NoAutorizadoException extends RuntimeException {
        public NoAutorizadoException(String msg) { super(msg); }
    }
    public static class FueraDePlazoException extends RuntimeException {
        public FueraDePlazoException(String msg) { super(msg); }
    }
    public static class YaCanceladoException extends RuntimeException {
        public YaCanceladoException(String msg) { super(msg); }
    }
    public static class EstadoInvalidoException extends RuntimeException {
        public EstadoInvalidoException(String msg) { super(msg); }
    }
}