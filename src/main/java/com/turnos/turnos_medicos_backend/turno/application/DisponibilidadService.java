package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotDTO;
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

    public DisponibilidadService(DisponibilidadPort disponibilidadPort,
                                  AgendaMedicoRepository agendaMedicoRepository,
                                  TurnoRepository turnoRepository) {
        this.disponibilidadPort = disponibilidadPort;
        this.agendaMedicoRepository = agendaMedicoRepository;
        this.turnoRepository = turnoRepository;
    }

    public List<SlotDTO> generarSlots(Long medicoId, LocalDate semana) {
        LocalDate lunes = semana.with(DayOfWeek.MONDAY);

        List<AgendaMedico> agendas = agendaMedicoRepository.findByMedicoId(medicoId);
        List<SlotDTO> slots = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate fecha = lunes.plusDays(i);
            int diaSemana = fecha.getDayOfWeek().getValue();

            agendas.stream()
                    .filter(AgendaMedico::getActivo)
                    .filter(a -> a.getDiaSemana().equals(diaSemana))
                    .forEach(agenda -> {
                        LocalTime cursor = agenda.getHoraInicio();
                        while (cursor.isBefore(agenda.getHoraFin())) {
                            boolean disponible = disponibilidadPort.estaDisponible(medicoId, fecha, cursor);
                            boolean bloqueado = disponibilidadPort.estaBloqueado(medicoId, fecha);
                            slots.add(new SlotDTO(fecha, cursor, disponible, bloqueado));
                            cursor = cursor.plusMinutes(agenda.getDuracionMinutos());
                        }
                    });
        }

        return slots;
    }

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

    public boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora) {
        return disponibilidadPort.estaDisponible(medicoId, fecha, hora);
    }
}