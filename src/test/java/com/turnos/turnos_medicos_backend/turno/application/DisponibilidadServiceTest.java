package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock private DisponibilidadPort disponibilidadPort;
    @Mock private AgendaMedicoRepository agendaMedicoRepository;
    @Mock private TurnoRepository turnoRepository;

    @InjectMocks
    private DisponibilidadService disponibilidadService;

    private static final Long MEDICO_ID = 1L;
    private static final LocalDate SEMANA = LocalDate.of(2025, 5, 12);

    private AgendaMedico agendaLunes;

    @BeforeEach
    void setUp() {
        agendaLunes = AgendaMedico.builder()
                .id(1L)
                .diaSemana(1)
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(10, 0))
                .duracionMinutos(30)
                .activo(true)
                .build();
    }

    @Test
    void deberiaGenerarDosSlots_CuandoMedicoTieneAgendaDeUnaHoraEnLunes() {
        when(agendaMedicoRepository.findByMedicoId(MEDICO_ID)).thenReturn(List.of(agendaLunes));
        when(disponibilidadPort.estaDisponible(eq(MEDICO_ID), eq(SEMANA), any())).thenReturn(true);
        when(disponibilidadPort.estaBloqueado(eq(MEDICO_ID), eq(SEMANA))).thenReturn(false);

        List<SlotDTO> slots = disponibilidadService.generarSlots(MEDICO_ID, SEMANA);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).hora()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(1).hora()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void deberiaMarcarSlotComoNoDisponible_CuandoDisponibilidadPortRetornaFalse() {
        when(agendaMedicoRepository.findByMedicoId(MEDICO_ID)).thenReturn(List.of(agendaLunes));
        when(disponibilidadPort.estaDisponible(eq(MEDICO_ID), eq(SEMANA), any())).thenReturn(false);
        when(disponibilidadPort.estaBloqueado(eq(MEDICO_ID), eq(SEMANA))).thenReturn(false);

        List<SlotDTO> slots = disponibilidadService.generarSlots(MEDICO_ID, SEMANA);

        assertThat(slots).allMatch(s -> !s.disponible());
    }

    @Test
    void deberiaMarcarSlotComoBloqueado_CuandoExisteBloqueoDiaParaEsaFecha() {
        when(agendaMedicoRepository.findByMedicoId(MEDICO_ID)).thenReturn(List.of(agendaLunes));
        when(disponibilidadPort.estaDisponible(any(), any(), any())).thenReturn(true);
        when(disponibilidadPort.estaBloqueado(eq(MEDICO_ID), eq(SEMANA))).thenReturn(true);

        List<SlotDTO> slots = disponibilidadService.generarSlots(MEDICO_ID, SEMANA);

        assertThat(slots).allMatch(SlotDTO::bloqueado);
    }

    @Test
    void deberiaRetornarListaVacia_CuandoMedicoNoTieneAgendaConfigurada() {
        when(agendaMedicoRepository.findByMedicoId(MEDICO_ID)).thenReturn(List.of());

        List<SlotDTO> slots = disponibilidadService.generarSlots(MEDICO_ID, SEMANA);

        assertThat(slots).isEmpty();
        verifyNoInteractions(disponibilidadPort);
    }

    @Test
    void deberiaMarcarSlotComoNoDisponible_CuandoHayTurnoConfirmadoEnEseHorario() {
        List<SlotDTO> slots = List.of(new SlotDTO(SEMANA, LocalTime.of(9, 0), true, false));

        Turno turnoConfirmado = Turno.builder()
                .hora(LocalTime.of(9, 0))
                .estado(EstadoTurno.CONFIRMADO)
                .build();

        when(turnoRepository.findByMedicoIdAndFecha(MEDICO_ID, SEMANA))
                .thenReturn(List.of(turnoConfirmado));

        List<SlotDTO> resultado = disponibilidadService.filtrarOcupados(MEDICO_ID, slots);

        assertThat(resultado.get(0).disponible()).isFalse();
    }

    @Test
    void deberiaDejarSlotDisponible_CuandoNoHayTurnosEnEseHorario() {
        List<SlotDTO> slots = List.of(new SlotDTO(SEMANA, LocalTime.of(9, 0), true, false));

        when(turnoRepository.findByMedicoIdAndFecha(MEDICO_ID, SEMANA)).thenReturn(List.of());

        List<SlotDTO> resultado = disponibilidadService.filtrarOcupados(MEDICO_ID, slots);

        assertThat(resultado.get(0).disponible()).isTrue();
    }

    @Test
    void deberiaDelegarEnDisponibilidadPort_CuandoSeConsultaEstaDisponible() {
        LocalTime hora = LocalTime.of(9, 0);
        when(disponibilidadPort.estaDisponible(MEDICO_ID, SEMANA, hora)).thenReturn(true);

        boolean resultado = disponibilidadService.estaDisponible(MEDICO_ID, SEMANA, hora);

        assertThat(resultado).isTrue();
        verify(disponibilidadPort).estaDisponible(MEDICO_ID, SEMANA, hora);
    }
}