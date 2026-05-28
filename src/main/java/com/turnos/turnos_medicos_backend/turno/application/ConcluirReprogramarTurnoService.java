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

    public Turno cambiarEstado(Long turnoId, EstadoTurno nuevoEstado) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new TurnoNoEncontradoException("Turno no encontrado: " + turnoId));

        if (nuevoEstado != EstadoTurno.CONCLUIDA) {
            throw new EstadoInvalidoException("Solo se acepta el estado CONCLUIDA");
        }
        if (turno.getEstado() != EstadoTurno.CONFIRMADO) {
            throw new EstadoInvalidoException(
                    "El turno debe estar en CONFIRMADO para poder concluirse (estado actual: " + turno.getEstado() + ")");
        }

        turno.setEstado(EstadoTurno.CONCLUIDA);
        return turnoRepository.save(turno);
    }

    public Turno reprogramar(Long turnoId, LocalDate nuevaFecha, LocalTime nuevaHora) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new TurnoNoEncontradoException("Turno no encontrado: " + turnoId));

        if (turno.getEstado() == EstadoTurno.CONCLUIDA || turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new EstadoInvalidoException(
                    "No se puede reprogramar un turno con estado " + turno.getEstado());
        }

        if (!disponibilidadPort.estaDisponible(turno.getMedico().getId(), nuevaFecha, nuevaHora)) {
            throw new SlotOcupadoException("El slot solicitado no está disponible");
        }

        turno.setFecha(nuevaFecha);
        turno.setHora(nuevaHora);
        turno.setEstado(EstadoTurno.CONFIRMADO);
        return turnoRepository.save(turno);
    }

    public static class TurnoNoEncontradoException extends RuntimeException {
        public TurnoNoEncontradoException(String msg) { super(msg); }
    }

    public static class EstadoInvalidoException extends RuntimeException {
        public EstadoInvalidoException(String msg) { super(msg); }
    }

    public static class SlotOcupadoException extends RuntimeException {
        public SlotOcupadoException(String msg) { super(msg); }
    }
}
