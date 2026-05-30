package com.turnos.turnos_medicos_backend.secretaria.application;

import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import com.turnos.turnos_medicos_backend.paciente.domain.port.PacienteRepository;
import com.turnos.turnos_medicos_backend.secretaria.domain.port.RecepcionistaRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import com.turnos.turnos_medicos_backend.shared.ports.IEventPublisher;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import com.turnos.turnos_medicos_backend.medico.domain.port.MedicoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class SecretariaService {

    private final PacienteRepository pacienteRepository;
    private final TurnoRepository turnoRepository;
    private final DisponibilidadPort disponibilidadPort;
    private final MedicoRepository medicoRepository;
    private final RecepcionistaRepository recepcionistaRepository;
    private final IEventPublisher eventPublisher;

    public SecretariaService(PacienteRepository pacienteRepository,
                              TurnoRepository turnoRepository,
                              DisponibilidadPort disponibilidadPort,
                              MedicoRepository medicoRepository,
                              RecepcionistaRepository recepcionistaRepository,
                              IEventPublisher eventPublisher) {
        this.pacienteRepository = pacienteRepository;
        this.turnoRepository = turnoRepository;
        this.disponibilidadPort = disponibilidadPort;
        this.medicoRepository = medicoRepository;
        this.recepcionistaRepository = recepcionistaRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Paciente> buscarPaciente(String q) {
        if (q == null || q.trim().length() < 2) {
            throw new BusquedaInsuficienteException("La búsqueda debe tener al menos 2 caracteres");
        }
        String term = q.trim();
        Optional<Paciente> porCi = pacienteRepository.findByCi(term);
        if (porCi.isPresent()) {
            return List.of(porCi.get());
        }
        return pacienteRepository.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(term, term);
    }

    public Paciente registrarPaciente(String ci, String nombre, String apellido,
                                       LocalDate fechaNacimiento, String telefono, String email) {
        if (pacienteRepository.findByCi(ci.trim()).isPresent()) {
            throw new CIDuplicadoException("Ya existe un paciente con CI: " + ci);
        }
        Paciente paciente = Paciente.builder()
                .ci(ci.trim())
                .nombre(nombre)
                .apellido(apellido)
                .fechaNacimiento(fechaNacimiento)
                .telefono(telefono)
                .email(email)
                .build();
        return pacienteRepository.save(paciente);
    }

    public Turno agendarTurnoDirecto(Long pacienteId, Long medicoId,
                                      LocalDate fecha, LocalTime hora, Long recepcionistaId) {
        if (!disponibilidadPort.estaDisponible(medicoId, fecha, hora)) {
            throw new SlotOcupadoException("El slot solicitado no está disponible");
        }

        boolean pacienteTieneTurnoEseDia = turnoRepository.findByPacienteId(pacienteId).stream()
                .anyMatch(t -> t.getFecha().equals(fecha)
                        && t.getEstado() != EstadoTurno.CANCELADO
                        && t.getEstado() != EstadoTurno.EXPIRADO);
        if (pacienteTieneTurnoEseDia) {
            throw new TurnoDuplicadoException("El paciente ya tiene un turno activo el " + fecha);
        }

        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
        var medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado: " + medicoId));

        Turno turno = Turno.builder()
                .paciente(paciente)
                .medico(medico)
                .fecha(fecha)
                .hora(hora)
                .estado(EstadoTurno.CONFIRMADO)
                .agendadoPor(recepcionistaId)
                .build();

        Turno guardado = turnoRepository.save(turno);
        eventPublisher.publicar("TURNO_CONFIRMADO", guardado.getId());
        return guardado;
    }

    public Turno cancelarTurnoAdmin(Long turnoId, Long recepcionistaId, String motivo) {
        if (motivo == null || motivo.trim().length() < 10) {
            throw new MotivoInsuficienteException("El motivo debe tener al menos 10 caracteres");
        }
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado: " + turnoId));

        turno.setEstado(EstadoTurno.CANCELADO);
        turno.setCanceladoPor("recepcionista:" + recepcionistaId);
        turno.setMotivoCancelacion(motivo);
        turno.setCanceladoEn(LocalDateTime.now());

        Turno cancelado = turnoRepository.save(turno);
        eventPublisher.publicar("TURNO_CANCELADO", cancelado.getId());
        return cancelado;
    }

    public List<Turno> turnosHoy() {
        return turnoRepository.findByFecha(LocalDate.now());
    }

    // ── Excepciones de dominio ────────────────────────────────────────────────
    public static class CIDuplicadoException extends RuntimeException {
        public CIDuplicadoException(String msg) { super(msg); }
    }
    public static class BusquedaInsuficienteException extends RuntimeException {
        public BusquedaInsuficienteException(String msg) { super(msg); }
    }
    public static class MotivoInsuficienteException extends RuntimeException {
        public MotivoInsuficienteException(String msg) { super(msg); }
    }
    public static class SlotOcupadoException extends RuntimeException {
        public SlotOcupadoException(String msg) { super(msg); }
    }
    public static class TurnoDuplicadoException extends RuntimeException {
        public TurnoDuplicadoException(String msg) { super(msg); }
    }
}
