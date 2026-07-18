package com.supai.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@SpringBootApplication
public class App implements CommandLineRunner {	
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
	}

}