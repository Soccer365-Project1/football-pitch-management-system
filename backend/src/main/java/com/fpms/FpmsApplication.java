package com.fpms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FpmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FpmsApplication.class, args);
	}

}
