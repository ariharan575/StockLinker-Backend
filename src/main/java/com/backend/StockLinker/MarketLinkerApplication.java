package com.backend.StockLinker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MarketLinkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketLinkerApplication.class, args);
	}

}
