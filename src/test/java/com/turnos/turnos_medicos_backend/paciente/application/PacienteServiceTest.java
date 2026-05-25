package com.turnos.turnos_medicos_backend.paciente.application;

import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import com.turnos.turnos_medicos_backend.paciente.domain.port.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService pacienteService;

    private Paciente pacienteValido;

    @BeforeEach
    void setUp() {
        pacienteValido = Paciente.builder()
                .id(1L)
                .ci("12345678")
                .nombre("María")
                .apellido("García")
                .email("maria.garcia@email.com")
                .telefono("70012345")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .build();
    }

    // -------------------------------------------------------------------------
    // registrar()
    // -------------------------------------------------------------------------

    @Test
    void deberiaRegistrarPaciente_CuandoElCiNoExiste() {
        when(pacienteRepository.findByCi(pacienteValido.getCi()))
                .thenReturn(Optional.empty());
        when(pacienteRepository.save(pacienteValido))
                .thenReturn(pacienteValido);

        Paciente resultado = pacienteService.registrar(pacienteValido);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCi()).isEqualTo("12345678");
        assertThat(resultado.getNombre()).isEqualTo("María");
        verify(pacienteRepository).save(pacienteValido);
    }

    @Test
    void deberiaLanzarExcepcion_CuandoElCiYaEstaRegistrado() {
        when(pacienteRepository.findByCi(pacienteValido.getCi()))
                .thenReturn(Optional.of(pacienteValido));

        assertThatThrownBy(() -> pacienteService.registrar(pacienteValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un paciente con ese CI");

        verify(pacienteRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // buscarPorCi()
    // -------------------------------------------------------------------------

    @Test
    void deberiaDevolverPaciente_CuandoBuscarPorCiExiste() {
        when(pacienteRepository.findByCi("12345678"))
                .thenReturn(Optional.of(pacienteValido));

        Paciente resultado = pacienteService.buscarPorCi("12345678");

        assertThat(resultado.getCi()).isEqualTo("12345678");
        verify(pacienteRepository).findByCi("12345678");
    }

    @Test
    void deberiaLanzarExcepcion_CuandoBuscarPorCiNoEncuentraNada() {
        when(pacienteRepository.findByCi("99999999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.buscarPorCi("99999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Paciente no encontrado");
    }

    // -------------------------------------------------------------------------
    // buscarPorNombre()
    // -------------------------------------------------------------------------

    @Test
    void deberiaDevolverListaPacientes_CuandoBuscarPorNombreEncuentraCoincidencias() {
        when(pacienteRepository.findByNombreContaining("María"))
                .thenReturn(List.of(pacienteValido));

        List<Paciente> resultado = pacienteService.buscarPorNombre("María");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("María");
    }

    @Test
    void deberiaDevolverListaVacia_CuandoBuscarPorNombreNoEncuentraNada() {
        when(pacienteRepository.findByNombreContaining("Inexistente"))
                .thenReturn(List.of());

        List<Paciente> resultado = pacienteService.buscarPorNombre("Inexistente");

        assertThat(resultado).isEmpty();
    }

    // -------------------------------------------------------------------------
    // buscarPorId()
    // -------------------------------------------------------------------------

    @Test
    void deberiaDevolverPaciente_CuandoBuscarPorIdExiste() {
        when(pacienteRepository.findById(1L))
                .thenReturn(Optional.of(pacienteValido));

        Paciente resultado = pacienteService.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(pacienteRepository).findById(1L);
    }

    @Test
    void deberiaLanzarExcepcion_CuandoBuscarPorIdNoExiste() {
        when(pacienteRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.buscarPorId(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Paciente no encontrado");
    }
}
