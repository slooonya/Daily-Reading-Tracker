package com.cpt202.dailyreadingtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DailyreadingtrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DailyreadingtrackerApplication.class, args);
	}

}
