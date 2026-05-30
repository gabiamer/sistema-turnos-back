package com.turnos.turnos_medicos_backend.admin.application;

import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private TurnoRepository turnoRepository;

    @InjectMocks
    private ReporteService reporteService;

    private static final LocalDate HOY    = LocalDate.now();
    private static final LocalDate INICIO = HOY.minusDays(10);
    private static final LocalDate FIN    = HOY;

    private Medico medico;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        medico = Medico.builder()
                .nombre("Carlos").apellido("Romero")
                .especialidad("Cardiologia").email("c@clinica.com").build();
        paciente = Paciente.builder()
                .ci("12345678").nombre("Maria").apellido("Lopez")
                .email("m@mail.com").build();
    }

    // ── kpisDelDia ────────────────────────────────────────────────────────────

    @Test
    void kpisDelDia_conTurnosMixtos_retornaConteosCorrecto() {
        Turno confirmado = turnoConEstado(EstadoTurno.CONFIRMADO, INICIO);
        Turno cancelado  = turnoConEstado(EstadoTurno.CANCELADO,  INICIO);
        Turno concluido  = turnoConEstado(EstadoTurno.CONCLUIDA,  INICIO);
        when(turnoRepository.findByFecha(HOY)).thenReturn(List.of(confirmado, cancelado, concluido));

        var kpis = reporteService.kpisDelDia(HOY);

        assertThat(kpis.confirmados()).isEqualTo(1);
        assertThat(kpis.cancelados()).isEqualTo(1);
        assertThat(kpis.concluidos()).isEqualTo(1);
    }

    // ── validarRango → 400 si > 90 días ──────────────────────────────────────

    @Test
    void reporteOcupacion_rangoMayorA90Dias_lanzaExcepcion() {
        LocalDate inicio = LocalDate.now().minusDays(100);
        LocalDate fin    = LocalDate.now();

        assertThatThrownBy(() -> reporteService.reporteOcupacion(inicio, fin))
                .isInstanceOf(ReporteService.RangoInvalidoException.class)
                .hasMessageContaining("90");
    }

    // ── reporteOcupacion con datos reales ────────────────────────────────────

    @Test
    void reporteOcupacion_conTurnosConfirmados_retornaOcupacionPositiva() {
        Turno t1 = turnoConEstado(EstadoTurno.CONFIRMADO, INICIO);
        Turno t2 = turnoConEstado(EstadoTurno.CANCELADO,  INICIO);
        when(turnoRepository.findByFechaBetween(INICIO, FIN)).thenReturn(List.of(t1, t2));

        var result = reporteService.reporteOcupacion(INICIO, FIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ocupados()).isEqualTo(1);
        assertThat(result.get(0).totalSlots()).isEqualTo(2);
    }

    // ── rol incorrecto → 400 en validarRango ─────────────────────────────────

    @Test
    void validarRango_finAntesQueInicio_lanzaExcepcion() {
        assertThatThrownBy(() -> reporteService.validarRango(FIN, INICIO))
                .isInstanceOf(ReporteService.RangoInvalidoException.class);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Turno turnoConEstado(EstadoTurno estado, LocalDate fecha) {
        return Turno.builder()
                .medico(medico)
                .paciente(paciente)
                .fecha(fecha)
                .hora(LocalTime.of(9, 0))
                .estado(estado)
                .build();
    }
}