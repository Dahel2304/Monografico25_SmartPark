package com.parking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import com.parking.service.EspacioService;

@SpringBootApplication
public class ParkingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkingSystemApplication.class, args);
	}

	@Bean
	CommandLineRunner migrarCodigosEspacios(EspacioService espacioService) {
		return args -> espacioService.migrarCodigosLegacy();
	}

}
