package com.turnos.turnos_medicos_backend;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.model.BloqueoDia;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import com.turnos.turnos_medicos_backend.agenda.domain.port.BloqueoDiaRepository;
import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import com.turnos.turnos_medicos_backend.medico.domain.port.MedicoRepository;
import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import com.turnos.turnos_medicos_backend.paciente.domain.port.PacienteRepository;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final AgendaMedicoRepository agendaMedicoRepository;
    private final BloqueoDiaRepository bloqueoDiaRepository;
    private final TurnoRepository turnoRepository;

    public DataSeeder(MedicoRepository medicoRepository,
                      PacienteRepository pacienteRepository,
                      AgendaMedicoRepository agendaMedicoRepository,
                      BloqueoDiaRepository bloqueoDiaRepository,
                      TurnoRepository turnoRepository) {
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.agendaMedicoRepository = agendaMedicoRepository;
        this.bloqueoDiaRepository = bloqueoDiaRepository;
        this.turnoRepository = turnoRepository;
    }

    @Override
    public void run(String... args) {
        try {
            // Solo carga si no hay médicos
            if (medicoRepository.findAll().isEmpty()) {
                cargarDatos();
            } else {
                System.out.println("ℹ️  DataSeeder: tablas ya tienen datos, se omite la carga");
            }
        } catch (Exception e) {
            System.out.println("❌ DataSeeder error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarDatos() {
        // ── Médicos ──────────────────────────────────────────
        Medico m1 = medicoRepository.save(Medico.builder()
                .nombre("Carlos").apellido("Romero")
                .especialidad("Cardiologia").email("cromero@clinica.com").build());

        Medico m2 = medicoRepository.save(Medico.builder()
                .nombre("Sofia").apellido("Vargas")
                .especialidad("Pediatria").email("svargas@clinica.com").build());

        Medico m3 = medicoRepository.save(Medico.builder()
                .nombre("Luis").apellido("Mendoza")
                .especialidad("Dermatologia").email("lmendoza@clinica.com").build());

        // ── Pacientes ─────────────────────────────────────────
        Paciente p1 = pacienteRepository.save(Paciente.builder()
                .ci("12345678").nombre("Maria").apellido("Lopez")
                .email("mlopez@mail.com").telefono("71234567")
                .fechaNacimiento(LocalDate.of(1990, 3, 15)).build());

        Paciente p2 = pacienteRepository.save(Paciente.builder()
                .ci("87654321").nombre("Pedro").apellido("Gutierrez")
                .email("pgutierrez@mail.com").telefono("79876543")
                .fechaNacimiento(LocalDate.of(1985, 7, 22)).build());

        // ── Agendas ───────────────────────────────────────────
        for (int dia = 1; dia <= 5; dia++) {
            agendaMedicoRepository.save(AgendaMedico.builder()
                    .medico(m1).diaSemana(dia)
                    .horaInicio(LocalTime.of(9, 0))
                    .horaFin(LocalTime.of(13, 0))
                    .duracionMinutos(30).activo(true).build());
        }

        for (int dia : new int[]{1, 3, 5}) {
            agendaMedicoRepository.save(AgendaMedico.builder()
                    .medico(m2).diaSemana(dia)
                    .horaInicio(LocalTime.of(8, 0))
                    .horaFin(LocalTime.of(12, 0))
                    .duracionMinutos(20).activo(true).build());
        }

        for (int dia : new int[]{2, 4}) {
            agendaMedicoRepository.save(AgendaMedico.builder()
                    .medico(m3).diaSemana(dia)
                    .horaInicio(LocalTime.of(15, 0))
                    .horaFin(LocalTime.of(19, 0))
                    .duracionMinutos(45).activo(true).build());
        }

        // ── Bloqueo ───────────────────────────────────────────
        bloqueoDiaRepository.save(BloqueoDia.builder()
                .medico(m1)
                .fechaInicio(LocalDate.of(2026, 6, 2))
                .fechaFin(LocalDate.of(2026, 6, 2))
                .motivo("Congreso de Cardiologia").build());

        // ── Turnos ────────────────────────────────────────────
        turnoRepository.save(Turno.builder()
                .medico(m1).paciente(p1)
                .fecha(LocalDate.of(2026, 6, 1))
                .hora(LocalTime.of(9, 0))
                .estado(EstadoTurno.CONFIRMADO).build());

        turnoRepository.save(Turno.builder()
                .medico(m2).paciente(p2)
                .fecha(LocalDate.of(2026, 6, 3))
                .hora(LocalTime.of(8, 0))
                .estado(EstadoTurno.PENDIENTE).build());

        turnoRepository.save(Turno.builder()
                .medico(m1).paciente(p2)
                .fecha(LocalDate.of(2026, 6, 4))
                .hora(LocalTime.of(10, 0))
                .estado(EstadoTurno.CANCELADO)
                .canceladoPor("paciente")
                .motivoCancelacion("No puede asistir").build());

        System.out.println("✅ DataSeeder: datos de prueba cargados");
    }
}