package com.codaily;

import com.codaily.config.CodailyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(CodailyProperties.class)
public class CodailyApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodailyApplication.class, args);
	}

}
