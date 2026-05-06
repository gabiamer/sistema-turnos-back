package com.turnos.turnos_medicos_backend.agenda.domain.model;

import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "bloqueo_dia")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(length = 255)
    private String motivo;
}