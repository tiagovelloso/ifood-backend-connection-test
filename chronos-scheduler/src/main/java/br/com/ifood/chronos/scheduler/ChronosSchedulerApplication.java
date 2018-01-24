package br.com.ifood.chronos.scheduler;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ChronosSchedulerApplication {
	
	public static void main(String[] args) {
		new SpringApplicationBuilder(ChronosSchedulerApplication.class).build().run(args);
	}
	
}