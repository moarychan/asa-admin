package com.azure.spring.asa.component.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient
public class AsaComponentBootAdminServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsaComponentBootAdminServerApplication.class, args);
	}

}
