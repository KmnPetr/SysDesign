package com.messenger.loadtest;

import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootApplication
public class LoadtestApplication {
	static int COUNT_USERS = 10_000;
	static int BATCH_SIZE = 1000;
	static int MIN_OWN_CHAT = 1;
	static int MAX_OWN_CHAT = 20;

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(LoadtestApplication.class, args);


		if (true){
			UserCreator userCreator = context.getBean(UserCreator.class);
			ChatsCreator chatsCreator = context.getBean(ChatsCreator.class);

			userCreator.createUsersInDB(COUNT_USERS);
			chatsCreator.createChatsAndUsersChats();
		} else {
			deleteAllTables(context);
		}

		int exitCode = SpringApplication.exit(context, () -> 0);
		System.exit(exitCode);
	}

	private static void deleteAllTables(ConfigurableApplicationContext context) {
		EntityManager entityManager = context.getBean(EntityManager.class);
		TransactionTemplate transactionTemplate = new TransactionTemplate(
				context.getBean(PlatformTransactionManager.class)
		);

		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createNativeQuery("DROP TABLE IF EXISTS flyway_schema_history").executeUpdate();
			entityManager.createNativeQuery("DROP TABLE IF EXISTS users_chats").executeUpdate();
			entityManager.createNativeQuery("DROP TABLE IF EXISTS messages").executeUpdate();
			entityManager.createNativeQuery("DROP TABLE IF EXISTS chats").executeUpdate();
			entityManager.createNativeQuery("DROP TABLE IF EXISTS users").executeUpdate();
		});
	}
}
