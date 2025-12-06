package com.xenotask.xeno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // added

@SpringBootApplication
@EnableScheduling // enable scheduled tasks
public class XenoApplication {

	public static void main(String[] args) {
		SpringApplication.run(XenoApplication.class, args);
	}

}
