package br.com.fiap.soat15.osservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OsServiceApplication.class, args);
	}

}
