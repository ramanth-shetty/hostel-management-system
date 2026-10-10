package com.hostel.hostel_management;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.hostel.entity.SystemSetting;
import com.hostel.repository.SystemSettingRepository;
import com.hostel.service.SettingService;

@SpringBootApplication(scanBasePackages = "com.hostel")
@EnableJpaRepositories(basePackages = "com.hostel.repository")
@EntityScan(basePackages = "com.hostel.entity")
public class HostelManagementApplication {

	/** Creates the standard_monthly_fee row on first start. Later changes are made through /api/settings. */
	@Bean
	CommandLineRunner initializeStandardMonthlyFee(
			SystemSettingRepository systemSettingRepository,
			@Value("${hostel.standard-monthly-fee}") BigDecimal standardMonthlyFee) {
		return args -> {
			if (systemSettingRepository.findBySettingName(SettingService.STANDARD_MONTHLY_FEE).isEmpty()) {
				systemSettingRepository.save(new SystemSetting(
						SettingService.STANDARD_MONTHLY_FEE,
						standardMonthlyFee.setScale(2, RoundingMode.HALF_UP)));
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(HostelManagementApplication.class, args);
	}

}
