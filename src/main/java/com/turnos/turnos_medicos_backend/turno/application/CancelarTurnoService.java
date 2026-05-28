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
    private final NotificacionTurnoService notificacionService;

    public CancelarTurnoService(TurnoRepository turnoRepository,
                                 IEventPublisher eventPublisher,
                                 NotificacionTurnoService notificacionService) {
        this.turnoRepository = turnoRepository;
        this.eventPublisher = eventPublisher;
        this.notificacionService = notificacionService;
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

    /** CU-médico: cancela el turno y notifica al paciente por los canales indicados */
    public Turno cancelarPorMedico(Long turnoId, String motivo, List<String> canales) {
        if (motivo == null || motivo.isBlank()) {
            throw new MotivoRequeridoException("El motivo de cancelación es requerido");
        }

        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        turno.setEstado(EstadoTurno.CANCELADO);
        turno.setCanceladoPor("MEDICO");
        turno.setMotivoCancelacion(motivo);
        turno.setCanceladoEn(LocalDateTime.now());

        Turno cancelado = turnoRepository.save(turno);
        notificacionService.notificarCancelacion(cancelado, motivo, canales);
        eventPublisher.publicar("TURNO_CANCELADO", cancelado.getId());
        return cancelado;
    }

    // ── Excepciones de dominio ────────────────────────────────────────────────
    public static class NoAutorizadoException extends RuntimeException {
        public NoAutorizadoException(String msg) { super(msg); }
    }
    public static class FueraDePlazoException extends RuntimeException {
        public FueraDePlazoException(String msg) { super(msg); }
    }
    public static class MotivoRequeridoException extends RuntimeException {
        public MotivoRequeridoException(String msg) { super(msg); }
    }
}