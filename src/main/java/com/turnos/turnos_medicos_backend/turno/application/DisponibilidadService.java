package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotDTO;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotMedicoDTO;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotMedicoDTO.TurnoResumen;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotMedicoDTO.PacienteResumen;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // ── NUEVO: slots enriquecidos para la vista del médico ────────────────────────

    /**
     * Genera la grilla semanal para la vista del médico.
     * Cada slot ocupado incluye los datos completos del turno y del paciente,
     * para que el front pueda mostrar el nombre del paciente y gestionar la cita.
     *
     * Estados que se muestran como "ocupado con turno":
     *   CONFIRMADO, PENDIENTE, BLOQUEADO, CONCLUIDA (histórico visible)
     * No se muestran: CANCELADO, EXPIRADO (no ocupan el slot visualmente)
     */
    public List<SlotMedicoDTO> generarSlotsParaMedico(Long medicoId, LocalDate semana) {
        LocalDate lunes = semana.with(DayOfWeek.MONDAY);
        List<AgendaMedico> agendas = agendaMedicoRepository.findByMedicoId(medicoId);

        // Cargar todos los turnos del médico una sola vez y agrupar por fecha+hora
        // para evitar N+1 queries al iterar los slots
        List<Turno> turnosMedico = turnoRepository.findByMedicoId(medicoId);

        Map<String, Turno> turnosPorFechaHora = turnosMedico.stream()
                .filter(t -> esEstadoVisible(t.getEstado()))
                .collect(Collectors.toMap(
                        t -> t.getFecha() + "__" + t.getHora(),
                        t -> t,
                        // Si hay duplicados (no debería), quedarse con el más reciente
                        (a, b) -> a.getId() > b.getId() ? a : b
                ));

        List<SlotMedicoDTO> resultado = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate fecha = lunes.plusDays(i);
            int diaSemana = fecha.getDayOfWeek().getValue();
            boolean diaBloqueado = disponibilidadPort.estaBloqueado(medicoId, fecha);

            agendas.stream()
                    .filter(AgendaMedico::getActivo)
                    .filter(a -> a.getDiaSemana().equals(diaSemana))
                    .forEach(agenda -> {
                        LocalTime cursor = agenda.getHoraInicio();
                        while (cursor.isBefore(agenda.getHoraFin())) {
                            String clave = fecha + "__" + cursor;
                            Turno turno = turnosPorFechaHora.get(clave);

                            if (diaBloqueado) {
                                resultado.add(SlotMedicoDTO.libre(fecha, cursor, true));
                            } else if (turno != null) {
                                TurnoResumen resumen = new TurnoResumen(
                                        turno.getId(),
                                        turno.getEstado().name(),
                                        new PacienteResumen(
                                                turno.getPaciente().getId(),
                                                turno.getPaciente().getNombre(),
                                                turno.getPaciente().getApellido(),
                                                turno.getPaciente().getCi()
                                        )
                                );
                                resultado.add(SlotMedicoDTO.ocupado(fecha, cursor, resumen));
                            } else {
                                resultado.add(SlotMedicoDTO.libre(fecha, cursor, false));
                            }

                            cursor = cursor.plusMinutes(agenda.getDuracionMinutos());
                        }
                    });
        }

        return resultado;
    }

    private boolean esEstadoVisible(EstadoTurno estado) {
        return estado == EstadoTurno.CONFIRMADO
            || estado == EstadoTurno.PENDIENTE
            || estado == EstadoTurno.BLOQUEADO
            || estado == EstadoTurno.CONCLUIDA;
        // CANCELADO y EXPIRADO no se muestran — liberan el slot visualmente
    }
}