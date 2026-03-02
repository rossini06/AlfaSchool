package br.com.alfaschool.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AlfaschoolBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlfaschoolBackendApplication.class, args);
	}

}
