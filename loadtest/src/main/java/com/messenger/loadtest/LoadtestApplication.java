package com.messenger.loadtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoadtestApplication {
	public static int COUNT_USERS = 1000;
	public static int BATCH_SIZE = 1000;
	public static int MIN_OWN_CHAT = 1;
	public static int MAX_OWN_CHAT = 10;
	public static int MIN_COUNT_MSG = 1;
	public static int MAX_COUNT_MSG = 40;

	public static void main(String[] args) {
		SpringApplication.run(LoadtestApplication.class, args);
	}
}
