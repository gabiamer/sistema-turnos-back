package com.turnos.turnos_medicos_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TurnosMedicosBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TurnosMedicosBackendApplication.class, args);
	}

}
