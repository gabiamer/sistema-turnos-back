package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.model.BloqueoDia;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import com.turnos.turnos_medicos_backend.agenda.domain.port.BloqueoDiaRepository;
import com.turnos.turnos_medicos_backend.shared.ports.DisponibilidadPort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock private DisponibilidadPort disponibilidadPort;
    @Mock private AgendaMedicoRepository agendaMedicoRepository;
    @Mock private TurnoRepository turnoRepository;
    @Mock private BloqueoDiaRepository bloqueoDiaRepository;

    @InjectMocks
    private DisponibilidadService disponibilidadService;

    private static final Long MEDICO_ID = 1L;

    // Lunes de una semana de prueba
    private static final LocalDate SEMANA = LocalDate.of(2025, 5, 12);

    private AgendaMedico agendaLunes;

    @BeforeEach
    void setUp() {
        // Agenda: lunes (1), 09:00 – 10:00, slots de 30 min → slots: 09:00 y 09:30
        agendaLunes = AgendaMedico.builder()
                .id(1L)
                .diaSemana(1)
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(10, 0))
                .duracionMinutos(30)
                .activo(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // generarSlots()
    // -------------------------------------------------------------------------

    @Test
    void deberiaGenerarDosSlots_CuandoMedicoTieneAgendaDeUnaHoraEnLunes() {
        when(agendaMedicoRepository.findByMedicoId(MEDICO_ID))
                .thenReturn(List.of(agendaLunes));
        when(disponibilidadPort.estaDisponible(eq(MEDICO_ID), eq(SEMANA), any()))
                .thenReturn(true);
        when(bloqueoDiaRepository.findByMedicoIdAndFechaOverlap(any(), any(), any()))
                .thenReturn(List.of());

        List<DisponibilidadService.SlotDTO> slots =
                disponibilidadService.generarSlots(MEDICO_ID, SEMANA);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).hora()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(1).hora()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void deberiaMarcarSlotComoNoDisponible_CuandoDisponibilidadPortRetornaFalse() {
        when(agendaMedicoRepository.findByMedicoId(MEDICO_ID))
                .thenReturn(List.of(agendaLunes));
        when(disponibilidadPort.estaDisponible(eq(MEDICO_ID), eq(SEMANA), any()))
                .thenReturn(false);
        when(bloqueoDiaRepository.findByMedicoIdAndFechaOverlap(any(), any(), any()))
                .thenReturn(List.of());

        List<DisponibilidadService.SlotDTO> slots =
                disponibilidadService.generarSlots(MEDICO_ID, SEMANA);

        assertThat(slots).allMatch(s -> !s.disponible());
    }

    @Test
    void deberiaMarcarSlotComoBloqueado_CuandoExisteBloqueoDiaParaEsaFecha() {
        when(agendaMedicoRepository.findByMedicoId(MEDICO_ID))
                .thenReturn(List.of(agendaLunes));
        when(disponibilidadPort.estaDisponible(any(), any(), any()))
                .thenReturn(true);
        when(bloqueoDiaRepository.findByMedicoIdAndFechaOverlap(
                eq(MEDICO_ID), eq(SEMANA), eq(SEMANA)))
                .thenReturn(List.of(new BloqueoDia()));

        List<DisponibilidadService.SlotDTO> slots =
                disponibilidadService.generarSlots(MEDICO_ID, SEMANA);

        assertThat(slots).allMatch(DisponibilidadService.SlotDTO::bloqueado);
    }

    @Test
    void deberiaRetornarListaVacia_CuandoMedicoNoTieneAgendaConfigurada() {
        when(agendaMedicoRepository.findByMedicoId(MEDICO_ID))
                .thenReturn(List.of());

        List<DisponibilidadService.SlotDTO> slots =
                disponibilidadService.generarSlots(MEDICO_ID, SEMANA);

        assertThat(slots).isEmpty();
        verifyNoInteractions(disponibilidadPort);
    }

    // -------------------------------------------------------------------------
    // filtrarOcupados()
    // -------------------------------------------------------------------------

    @Test
    void deberiaMarcarSlotComoNoDisponible_CuandoHayTurnoConfirmadoEnEseHorario() {
        List<DisponibilidadService.SlotDTO> slots = List.of(
                new DisponibilidadService.SlotDTO(SEMANA, LocalTime.of(9, 0), true, false)
        );

        Turno turnoConfirmado = Turno.builder()
                .hora(LocalTime.of(9, 0))
                .estado(EstadoTurno.CONFIRMADO)
                .build();

        when(turnoRepository.findByMedicoIdAndFecha(MEDICO_ID, SEMANA))
                .thenReturn(List.of(turnoConfirmado));

        List<DisponibilidadService.SlotDTO> resultado =
                disponibilidadService.filtrarOcupados(MEDICO_ID, slots);

        assertThat(resultado.get(0).disponible()).isFalse();
    }

    @Test
    void deberiaDejarSlotDisponible_CuandoNoHayTurnosEnEseHorario() {
        List<DisponibilidadService.SlotDTO> slots = List.of(
                new DisponibilidadService.SlotDTO(SEMANA, LocalTime.of(9, 0), true, false)
        );

        when(turnoRepository.findByMedicoIdAndFecha(MEDICO_ID, SEMANA))
                .thenReturn(List.of());

        List<DisponibilidadService.SlotDTO> resultado =
                disponibilidadService.filtrarOcupados(MEDICO_ID, slots);

        assertThat(resultado.get(0).disponible()).isTrue();
    }

    // -------------------------------------------------------------------------
    // estaDisponible() — delega en el puerto
    // -------------------------------------------------------------------------

    @Test
    void deberiaDelegarEnDisponibilidadPort_CuandoSeConsultaEstaDisponible() {
        LocalTime hora = LocalTime.of(9, 0);
        when(disponibilidadPort.estaDisponible(MEDICO_ID, SEMANA, hora)).thenReturn(true);

        boolean resultado = disponibilidadService.estaDisponible(MEDICO_ID, SEMANA, hora);

        assertThat(resultado).isTrue();
        verify(disponibilidadPort).estaDisponible(MEDICO_ID, SEMANA, hora);
    }
}