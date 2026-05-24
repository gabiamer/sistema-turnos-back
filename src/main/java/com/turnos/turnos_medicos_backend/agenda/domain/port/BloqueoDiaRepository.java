// /agenda/domain/port/BloqueoDiaRepository.java
package com.turnos.turnos_medicos_backend.agenda.domain.port;

import com.turnos.turnos_medicos_backend.agenda.domain.model.BloqueoDia;
import java.time.LocalDate;
import java.util.List;

public interface BloqueoDiaRepository {

    BloqueoDia save(BloqueoDia bloqueo);

    List<BloqueoDia> findByMedicoId(Long medicoId);

    // Busca bloqueos que se solapan con un rango de fechas dado
    List<BloqueoDia> findByMedicoIdAndFechaOverlap(Long medicoId, LocalDate desde, LocalDate hasta);
}