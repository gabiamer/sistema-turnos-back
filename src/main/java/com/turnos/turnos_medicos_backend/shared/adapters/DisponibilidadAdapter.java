// /shared/adapters/DisponibilidadAdapter.java
package com.turnos.turnos_medicos_backend.shared.adapters;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import com.turnos.turnos_medicos_backend.agenda.domain.port.BloqueoDiaRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DisponibilidadAdapter implements DisponibilidadPort {

    private final AgendaMedicoRepository agendaMedicoRepository;
    private final BloqueoDiaRepository bloqueoDiaRepository;

    public DisponibilidadAdapter(AgendaMedicoRepository agendaMedicoRepository,
                                  BloqueoDiaRepository bloqueoDiaRepository) {
        this.agendaMedicoRepository = agendaMedicoRepository;
        this.bloqueoDiaRepository = bloqueoDiaRepository;
    }

    @Override
    public boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora) {
        int diaSemana = fecha.getDayOfWeek().getValue();

        List<AgendaMedico> agendas = agendaMedicoRepository.findByMedicoId(medicoId);

        boolean tieneSlot = agendas.stream()
                .filter(a -> a.getActivo())
                .filter(a -> a.getDiaSemana().equals(diaSemana))
                .anyMatch(a ->
                        !hora.isBefore(a.getHoraInicio()) && hora.isBefore(a.getHoraFin())
                );

        if (!tieneSlot) {
            return false;
        }

        return bloqueoDiaRepository
                .findByMedicoIdAndFechaOverlap(medicoId, fecha, fecha)
                .isEmpty();
    }

    @Override
    public boolean estaBloqueado(Long medicoId, LocalDate fecha) {
        return !bloqueoDiaRepository
                .findByMedicoIdAndFechaOverlap(medicoId, fecha, fecha)
                .isEmpty();
    }
}