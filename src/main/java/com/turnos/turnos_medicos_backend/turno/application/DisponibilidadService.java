package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import com.turnos.turnos_medicos_backend.agenda.domain.port.BloqueoDiaRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DisponibilidadService {

    private final DisponibilidadPort disponibilidadPort;
    private final AgendaMedicoRepository agendaMedicoRepository;
    private final TurnoRepository turnoRepository;
    private final BloqueoDiaRepository bloqueoDiaRepository;

    public DisponibilidadService(DisponibilidadPort disponibilidadPort,
                                  AgendaMedicoRepository agendaMedicoRepository,
                                  TurnoRepository turnoRepository,
                                  BloqueoDiaRepository bloqueoDiaRepository) {
        this.disponibilidadPort = disponibilidadPort;
        this.agendaMedicoRepository = agendaMedicoRepository;
        this.turnoRepository = turnoRepository;
        this.bloqueoDiaRepository = bloqueoDiaRepository;
    }

    /**
     * Genera todos los slots de la semana que empieza en `semana` (lunes).
     * Cada slot indica si está disponible o bloqueado.
     */
    public List<SlotDTO> generarSlots(Long medicoId, LocalDate semana) {
        // Normalizar al lunes de esa semana
        LocalDate lunes = semana.with(DayOfWeek.MONDAY);

        List<AgendaMedico> agendas = agendaMedicoRepository.findByMedicoId(medicoId);
        List<SlotDTO> slots = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate fecha = lunes.plusDays(i);
            int diaSemana = fecha.getDayOfWeek().getValue(); // 1=Lun … 7=Dom

            agendas.stream()
                    .filter(AgendaMedico::getActivo)
                    .filter(a -> a.getDiaSemana().equals(diaSemana))
                    .forEach(agenda -> {
                        LocalTime cursor = agenda.getHoraInicio();
                        while (cursor.isBefore(agenda.getHoraFin())) {
                            boolean disponible = estaDisponible(medicoId, fecha, cursor);
                            boolean bloqueado = !bloqueoDiaRepository
                                    .findByMedicoIdAndFechaOverlap(medicoId, fecha, fecha)
                                    .isEmpty();
                            slots.add(new SlotDTO(fecha, cursor, disponible, bloqueado));
                            cursor = cursor.plusMinutes(agenda.getDuracionMinutos());
                        }
                    });
        }

        return slots;
    }

    /**
     * Filtra los slots ocupados por turnos CONFIRMADOS o PENDIENTES.
     */
    public List<SlotDTO> filtrarOcupados(Long medicoId, List<SlotDTO> slots) {
        return slots.stream()
                .map(slot -> {
                    boolean ocupado = turnoRepository
                            .findByMedicoIdAndFecha(medicoId, slot.fecha())
                            .stream()
                            .anyMatch(t ->
                                    t.getHora().equals(slot.hora()) &&
                                    (t.getEstado() == EstadoTurno.CONFIRMADO ||
                                     t.getEstado() == EstadoTurno.PENDIENTE)
                            );
                    if (ocupado) {
                        return new SlotDTO(slot.fecha(), slot.hora(), false, slot.bloqueado());
                    }
                    return slot;
                })
                .toList();
    }

    /**
     * Devuelve true si el slot está en la agenda del médico y no está bloqueado.
     * Delega en DisponibilidadPort (implementado por Alex en DisponibilidadAdapter).
     */
    public boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora) {
        return disponibilidadPort.estaDisponible(medicoId, fecha, hora);
    }

    // ── DTO interno ──────────────────────────────────────────────────────────

    public record SlotDTO(
            LocalDate fecha,
            LocalTime hora,
            boolean disponible,
            boolean bloqueado
    ) {}
}