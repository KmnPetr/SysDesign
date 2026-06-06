package com.messenger.loadtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class LoadtestApplication {
	static int countUsers = 10_000;

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(LoadtestApplication.class, args);
		UserCreator userCreator = context.getBean(UserCreator.class);

		userCreator.createUsersInDB(countUsers);

		int exitCode = SpringApplication.exit(context, () -> 0);
		System.exit(exitCode);
	}


}
