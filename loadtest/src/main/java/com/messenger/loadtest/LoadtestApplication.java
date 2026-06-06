package com.messenger.loadtest;

import com.messenger.loadtest.creators.ChatsCreator;
import com.messenger.loadtest.creators.MessagesCreator;
import com.messenger.loadtest.creators.UserCreator;
import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootApplication
public class LoadtestApplication {
	public static int COUNT_USERS = 20000;
	public static int BATCH_SIZE = 1000;
	public static int MIN_OWN_CHAT = 1;
	public static int MAX_OWN_CHAT = 10;
	public static int MIN_COUNT_MSG = 1;
	public static int MAX_COUNT_MSG = 40;

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(LoadtestApplication.class, args);


		if (true){
			context.getBean(ExistingIdOffsets.class);
			UserCreator userCreator = context.getBean(UserCreator.class);
			ChatsCreator chatsCreator = context.getBean(ChatsCreator.class);
			MessagesCreator messagesCreator = context.getBean(MessagesCreator.class);

			userCreator.createUsersInDB(COUNT_USERS);
			chatsCreator.createChatsAndUsersChats();
			messagesCreator.createMessagesInDB();
			printTableCounts(context);
		}

		int exitCode = SpringApplication.exit(context, () -> 0);
		System.exit(exitCode);
	}

	private static void printTableCounts(ConfigurableApplicationContext context) {
		EntityManager entityManager = context.getBean(EntityManager.class);
		TransactionTemplate transactionTemplate = new TransactionTemplate(
				context.getBean(PlatformTransactionManager.class)
		);

		transactionTemplate.executeWithoutResult(status -> {
			System.out.println("Записей в БД:");
			printStatRow("Таблица", "Кол-во", "");
			printTableStat(entityManager, "users", "id");
			printTableStat(entityManager, "chats", "id");
			printUsersChatsStat(entityManager);
			printTableStat(entityManager, "messages", "id");
			System.out.flush();
		});
	}

	private static void printStatRow(String table, String count, String idStats) {
		System.out.printf("%-15s\t%12s\t%s%n", table, count, idStats);
	}

	private static void printTableStat(EntityManager entityManager, String tableName, String idColumn) {
		long count = countRows(entityManager, tableName);
		Long minId = minColumn(entityManager, tableName, idColumn);
		Long maxId = maxColumn(entityManager, tableName, idColumn);
		String idStats = String.format("(id min=%s, max=%s)", formatId(minId), formatId(maxId));
		printStatRow(tableName, String.valueOf(count), idStats);
	}

	private static void printUsersChatsStat(EntityManager entityManager) {
		long count = countRows(entityManager, "users_chats");
		Long minUserId = minColumn(entityManager, "users_chats", "user_id");
		Long maxUserId = maxColumn(entityManager, "users_chats", "user_id");
		Long minChatId = minColumn(entityManager, "users_chats", "chat_id");
		Long maxChatId = maxColumn(entityManager, "users_chats", "chat_id");
		String idStats = String.format(
				"(user_id min=%s, max=%s; chat_id min=%s, max=%s)",
				formatId(minUserId), formatId(maxUserId), formatId(minChatId), formatId(maxChatId)
		);
		printStatRow("users_chats", String.valueOf(count), idStats);
	}

	private static String formatId(Long id) {
		return id == null ? "-" : id.toString();
	}

	private static long countRows(EntityManager entityManager, String tableName) {
		return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + tableName).getSingleResult()).longValue();
	}

	private static Long minColumn(EntityManager entityManager, String tableName, String columnName) {
		Object result = entityManager.createNativeQuery("SELECT MIN(" + columnName + ") FROM " + tableName).getSingleResult();
		return toLongOrNull(result);
	}

	private static Long maxColumn(EntityManager entityManager, String tableName, String columnName) {
		Object result = entityManager.createNativeQuery("SELECT MAX(" + columnName + ") FROM " + tableName).getSingleResult();
		return toLongOrNull(result);
	}

	private static Long toLongOrNull(Object value) {
		return value == null ? null : ((Number) value).longValue();
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
