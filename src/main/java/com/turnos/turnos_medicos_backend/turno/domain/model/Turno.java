package com.turnos.turnos_medicos_backend.turno.domain.model;

import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "turnos",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_paciente_fecha",
            columnNames = {"paciente_id", "fecha"}
        )
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoTurno estado = EstadoTurno.PENDIENTE;

    @Column(name = "bloqueo_expira")
    private LocalDateTime bloqueoExpira;

    @Column(name = "cancelado_por", length = 100)
    private String canceladoPor;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @Column(name = "cancelado_en")
    private LocalDateTime canceladoEn;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();
}