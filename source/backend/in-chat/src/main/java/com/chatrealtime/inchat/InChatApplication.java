package com.chatrealtime.inchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.chatrealtime")
@ConfigurationPropertiesScan(basePackages = "com.chatrealtime.security")
@EnableMongoRepositories(basePackages = "com.chatrealtime.repository")
public class InChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(InChatApplication.class, args);
	}

}
