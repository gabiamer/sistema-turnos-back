// /agenda/application/AgendaMedicoService.java
package com.turnos.turnos_medicos_backend.agenda.application;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.model.BloqueoDia;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import com.turnos.turnos_medicos_backend.agenda.domain.port.BloqueoDiaRepository;
import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import com.turnos.turnos_medicos_backend.medico.domain.port.MedicoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AgendaMedicoService {

    private final AgendaMedicoRepository agendaMedicoRepository;
    private final BloqueoDiaRepository bloqueoDiaRepository;
    private final MedicoRepository medicoRepository;

    public AgendaMedicoService(AgendaMedicoRepository agendaMedicoRepository,
                                BloqueoDiaRepository bloqueoDiaRepository,
                                MedicoRepository medicoRepository) {
        this.agendaMedicoRepository = agendaMedicoRepository;
        this.bloqueoDiaRepository = bloqueoDiaRepository;
        this.medicoRepository = medicoRepository;
    }

    public AgendaMedico guardarConfig(AgendaMedico agenda) {
        return agendaMedicoRepository.save(agenda);
    }

    public List<AgendaMedico> findByMedicoId(Long medicoId) {
        return agendaMedicoRepository.findByMedicoId(medicoId);
    }

    /**
     * Reemplaza toda la agenda del médico con la nueva configuración.
     * El controller ya verificó que no hay conflictos antes de llamar aquí.
     */
    public List<AgendaMedico> actualizarAgenda(Long medicoId, List<AgendaMedico> nuevaAgenda) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        // Desactivar la agenda anterior
        List<AgendaMedico> actual = agendaMedicoRepository.findByMedicoId(medicoId);
        actual.forEach(a -> {
            a.setActivo(false);
            agendaMedicoRepository.save(a);
        });

        // Guardar la nueva
        return nuevaAgenda.stream()
                .map(a -> {
                    a.setMedico(medico);
                    a.setActivo(true);
                    return agendaMedicoRepository.save(a);
                })
                .toList();
    }

    // Bloqueos
    public BloqueoDia guardarBloqueo(BloqueoDia bloqueo) {
        return bloqueoDiaRepository.save(bloqueo);
    }

    public List<BloqueoDia> findBloqueosByMedicoId(Long medicoId) {
        return bloqueoDiaRepository.findByMedicoId(medicoId);
    }

    public List<BloqueoDia> findBloqueosSolapados(Long medicoId, LocalDate desde, LocalDate hasta) {
        return bloqueoDiaRepository.findByMedicoIdAndFechaOverlap(medicoId, desde, hasta);
    }
}