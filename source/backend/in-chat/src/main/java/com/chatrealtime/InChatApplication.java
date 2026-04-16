package com.chatrealtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
@ConfigurationPropertiesScan
@ImportHttpServices(basePackages = "com.chatrealtime.client")
public class InChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(InChatApplication.class, args);
	}

}

