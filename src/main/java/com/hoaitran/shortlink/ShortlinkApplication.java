package com.hoaitran.shortlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShortlinkApplication {

	public static void main(String[] args) {
		System.setProperty("user.timezone", "GMT+7");
		SpringApplication.run(ShortlinkApplication.class, args);
	}

}
