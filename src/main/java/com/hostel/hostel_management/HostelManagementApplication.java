package com.hostel.hostel_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.hostel")
@EnableJpaRepositories(basePackages = "com.hostel.repository")
@EntityScan(basePackages = "com.hostel.entity")
public class HostelManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(HostelManagementApplication.class, args);
	}

}
