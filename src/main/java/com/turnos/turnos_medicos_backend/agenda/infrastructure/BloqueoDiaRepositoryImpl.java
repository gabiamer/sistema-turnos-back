// /agenda/infrastructure/BloqueoDiaRepositoryImpl.java
package com.turnos.turnos_medicos_backend.agenda.infrastructure;

import com.turnos.turnos_medicos_backend.agenda.domain.model.BloqueoDia;
import com.turnos.turnos_medicos_backend.agenda.domain.port.BloqueoDiaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BloqueoDiaRepositoryImpl
        extends JpaRepository<BloqueoDia, Long>, BloqueoDiaRepository {

    List<BloqueoDia> findByMedicoId(Long medicoId);

    // Un bloqueo se solapa si su inicio <= hasta Y su fin >= desde
    @Query("""
            SELECT b FROM BloqueoDia b
            WHERE b.medico.id = :medicoId
              AND b.fechaInicio <= :hasta
              AND b.fechaFin   >= :desde
            """)
    List<BloqueoDia> findByMedicoIdAndFechaOverlap(
            @Param("medicoId") Long medicoId,
            @Param("desde")    LocalDate desde,
            @Param("hasta")    LocalDate hasta
    );
}