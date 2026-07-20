package com.restaurantpos.pos_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class PosBackendApplication {

	public static void main(String[] args) {
		// The Railway server runs in a different region/timezone than Germany.
		// Force the JVM's default timezone to Europe/Berlin so every
		// LocalDateTime.now() call (order/payment timestamps) reflects the
		// restaurant's actual local time, regardless of where the container
		// physically runs.
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));

		SpringApplication.run(PosBackendApplication.class, args);
	}

}