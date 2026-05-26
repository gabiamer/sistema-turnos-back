package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.medico.domain.port.MedicoRepository;
import com.turnos.turnos_medicos_backend.paciente.domain.port.PacienteRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import com.turnos.turnos_medicos_backend.shared.ports.IEventPublisher;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class SolicitarTurnoService {

    private final TurnoRepository turnoRepository;
    private final DisponibilidadPort disponibilidadPort;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final IEventPublisher eventPublisher;

    public SolicitarTurnoService(TurnoRepository turnoRepository,
                                  DisponibilidadPort disponibilidadPort,
                                  MedicoRepository medicoRepository,
                                  PacienteRepository pacienteRepository,
                                  IEventPublisher eventPublisher) {
        this.turnoRepository = turnoRepository;
        this.disponibilidadPort = disponibilidadPort;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.eventPublisher = eventPublisher;
    }

    /** CU-01 paso 1: bloquea el slot por 5 minutos */
    public Turno bloquearHorario(Long pacienteId, Long medicoId, LocalDate fecha, LocalTime hora) {
        // Regla: slot debe estar disponible
        if (!disponibilidadPort.estaDisponible(medicoId, fecha, hora)) {
            throw new SlotOcupadoException("Slot ya ocupado");
        }

        // Regla: paciente no puede tener otro turno el mismo día (fitness function → uq_paciente_fecha)
        boolean tieneTurnoEseDia = turnoRepository.findByPacienteId(pacienteId)
                .stream()
                .anyMatch(t -> t.getFecha().equals(fecha)
                        && t.getEstado() != EstadoTurno.CANCELADO
                        && t.getEstado() != EstadoTurno.EXPIRADO);
        if (tieneTurnoEseDia) {
            throw new TurnoDuplicadoException("Paciente ya tiene turno ese día");
        }

        var medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));
        var paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        Turno turno = Turno.builder()
                .paciente(paciente)
                .medico(medico)
                .fecha(fecha)
                .hora(hora)
                .estado(EstadoTurno.BLOQUEADO)
                .bloqueoExpira(LocalDateTime.now().plusMinutes(5))
                .creadoEn(LocalDateTime.now())
                .build();

        Turno guardado = turnoRepository.save(turno);
        eventPublisher.publicar("TURNO_BLOQUEADO", guardado.getId());
        return guardado;
    }

    /** CU-01 paso 2: confirma el turno bloqueado */
    public Turno confirmarTurno(Long turnoId) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (turno.getEstado() != EstadoTurno.BLOQUEADO) {
            throw new BloqueoExpiradoException("Bloqueo expirado, slot liberado");
        }
        if (turno.getBloqueoExpira().isBefore(LocalDateTime.now())) {
            turno.setEstado(EstadoTurno.EXPIRADO);
            turnoRepository.save(turno);
            throw new BloqueoExpiradoException("Bloqueo expirado, slot liberado");
        }

        turno.setEstado(EstadoTurno.CONFIRMADO);
        turno.setBloqueoExpira(null);
        Turno confirmado = turnoRepository.save(turno);
        eventPublisher.publicar("TURNO_CONFIRMADO", confirmado.getId());
        return confirmado;
    }

    /** Job: libera bloqueos expirados cada 1 minuto */
    @Scheduled(fixedRate = 60_000)
    public void liberarBloqueosExpirados() {
        List<Turno> expirados = turnoRepository
                .findByEstadoAndBloqueoExpiraBefore(EstadoTurno.BLOQUEADO, LocalDateTime.now());
        expirados.forEach(t -> {
            t.setEstado(EstadoTurno.EXPIRADO);
            turnoRepository.save(t);
            eventPublisher.publicar("TURNO_EXPIRADO", t.getId());
        });
    }

    // ── Excepciones de dominio ────────────────────────────────────────────────
    public static class SlotOcupadoException extends RuntimeException {
        public SlotOcupadoException(String msg) { super(msg); }
    }
    public static class TurnoDuplicadoException extends RuntimeException {
        public TurnoDuplicadoException(String msg) { super(msg); }
    }
    public static class BloqueoExpiradoException extends RuntimeException {
        public BloqueoExpiradoException(String msg) { super(msg); }
    }
}