package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import com.turnos.turnos_medicos_backend.turno.domain.model.AgendaSlotDTO;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotDTO;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // ── Método original — sin cambios, sigue siendo usado por el endpoint público ──

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
                            boolean bloqueado  = disponibilidadPort.estaBloqueado(medicoId, fecha);
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
                    return ocupado ? new SlotDTO(slot.fecha(), slot.hora(), false, slot.bloqueado()) : slot;
                })
                .toList();
    }

    public boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora) {
        return disponibilidadPort.estaDisponible(medicoId, fecha, hora);
    }

    /** Vista del médico: slots de la semana enriquecidos con datos de turno y paciente */
    public List<AgendaSlotDTO> generarAgendaSemana(Long medicoId, LocalDate semana) {
        List<SlotDTO> base = generarSlots(medicoId, semana);

        return base.stream().map(slot -> {
            Optional<Turno> turnoOpt = turnoRepository
                    .findByMedicoIdAndFecha(medicoId, slot.fecha())
                    .stream()
                    .filter(t -> t.getHora().equals(slot.hora()) &&
                            (t.getEstado() == EstadoTurno.CONFIRMADO ||
                             t.getEstado() == EstadoTurno.PENDIENTE))
                    .findFirst();

            AgendaSlotDTO.TurnoInfo turnoInfo = turnoOpt.map(t ->
                new AgendaSlotDTO.TurnoInfo(
                    t.getId(),
                    t.getEstado().name(),
                    new AgendaSlotDTO.PacienteInfo(
                        t.getPaciente().getId(),
                        t.getPaciente().getNombre(),
                        t.getPaciente().getApellido(),
                        t.getPaciente().getCi()
                    )
                )
            ).orElse(null);

            boolean libreReal = turnoOpt.isEmpty() && slot.disponible() && !slot.bloqueado();
            return new AgendaSlotDTO(slot.fecha(), slot.hora(), libreReal, slot.bloqueado(), turnoInfo);
        }).toList();
    }
}