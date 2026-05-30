package com.turnos.turnos_medicos_backend.secretaria.application;

import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import com.turnos.turnos_medicos_backend.medico.domain.port.MedicoRepository;
import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import com.turnos.turnos_medicos_backend.paciente.domain.port.PacienteRepository;
import com.turnos.turnos_medicos_backend.secretaria.domain.port.RecepcionistaRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import com.turnos.turnos_medicos_backend.shared.ports.IEventPublisher;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecretariaServiceTest {

    @Mock private PacienteRepository pacienteRepository;
    @Mock private TurnoRepository turnoRepository;
    @Mock private DisponibilidadPort disponibilidadPort;
    @Mock private MedicoRepository medicoRepository;
    @Mock private RecepcionistaRepository recepcionistaRepository;
    @Mock private IEventPublisher eventPublisher;

    @InjectMocks
    private SecretariaService secretariaService;

    private static final Long PACIENTE_ID    = 1L;
    private static final Long MEDICO_ID      = 2L;
    private static final Long RECEP_ID       = 10L;
    private static final LocalDate HOY       = LocalDate.now();
    private static final LocalTime HORA      = LocalTime.of(9, 0);

    private Paciente paciente;
    private Medico medico;

    @BeforeEach
    void setUp() {
        paciente = Paciente.builder().ci("12345678")
                .nombre("Maria").apellido("Lopez").email("m@mail.com").build();
        medico = Medico.builder().nombre("Carlos").apellido("Romero")
                .especialidad("Cardiologia").email("c@clinica.com").build();
    }

    // ── agendarTurnoDirecto ───────────────────────────────────────────────────

    @Test
    void agendarTurnoDirecto_debeRetornarTurnoConEstadoConfirmado() {
        when(disponibilidadPort.estaDisponible(MEDICO_ID, HOY, HORA)).thenReturn(true);
        when(turnoRepository.findByPacienteId(PACIENTE_ID)).thenReturn(List.of());
        when(pacienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(MEDICO_ID)).thenReturn(Optional.of(medico));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));

        Turno resultado = secretariaService.agendarTurnoDirecto(PACIENTE_ID, MEDICO_ID, HOY, HORA, RECEP_ID);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTurno.CONFIRMADO);
        assertThat(resultado.getAgendadoPor()).isEqualTo(RECEP_ID);
        verify(eventPublisher).publicar("TURNO_CONFIRMADO", null);
    }

    @Test
    void agendarTurnoDirecto_cuandoPacienteYaTieneTurnoEseDia_debeLanzarTurnoDuplicadoException() {
        Turno turnoExistente = Turno.builder()
                .fecha(HOY).estado(EstadoTurno.CONFIRMADO).build();

        when(disponibilidadPort.estaDisponible(MEDICO_ID, HOY, HORA)).thenReturn(true);
        when(turnoRepository.findByPacienteId(PACIENTE_ID)).thenReturn(List.of(turnoExistente));

        assertThatThrownBy(() ->
                secretariaService.agendarTurnoDirecto(PACIENTE_ID, MEDICO_ID, HOY, HORA, RECEP_ID))
                .isInstanceOf(SecretariaService.TurnoDuplicadoException.class);

        verify(turnoRepository, never()).save(any());
    }

    @Test
    void agendarTurnoDirecto_cuandoSlotNoDisponible_debeLanzarSlotOcupadoException() {
        when(disponibilidadPort.estaDisponible(MEDICO_ID, HOY, HORA)).thenReturn(false);

        assertThatThrownBy(() ->
                secretariaService.agendarTurnoDirecto(PACIENTE_ID, MEDICO_ID, HOY, HORA, RECEP_ID))
                .isInstanceOf(SecretariaService.SlotOcupadoException.class);

        verify(turnoRepository, never()).save(any());
    }

    // ── cancelarTurnoAdmin ────────────────────────────────────────────────────

    @Test
    void cancelarTurnoAdmin_cuandoMotivoTiene5Chars_debeLanzarMotivoInsuficienteException() {
        assertThatThrownBy(() ->
                secretariaService.cancelarTurnoAdmin(1L, RECEP_ID, "corto"))
                .isInstanceOf(SecretariaService.MotivoInsuficienteException.class);

        verifyNoInteractions(turnoRepository);
    }

    @Test
    void cancelarTurnoAdmin_cuandoTurnoEsDeHaceUnaHora_noDebeLanzarExcepcion() {
        Turno turno = Turno.builder()
                .fecha(HOY)
                .hora(LocalTime.now().minusHours(1))
                .estado(EstadoTurno.CONFIRMADO)
                .build();

        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turno));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));

        Turno resultado = secretariaService.cancelarTurnoAdmin(1L, RECEP_ID, "Motivo suficientemente largo");

        assertThat(resultado.getEstado()).isEqualTo(EstadoTurno.CANCELADO);
        assertThat(resultado.getCanceladoPor()).isEqualTo("recepcionista:" + RECEP_ID);
    }
}
