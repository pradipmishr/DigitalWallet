package com.project.digitalwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DigitalwalletApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalwalletApplication.class, args);
	}

}
