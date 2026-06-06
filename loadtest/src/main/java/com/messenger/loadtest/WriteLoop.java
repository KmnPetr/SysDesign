package com.messenger.loadtest;

import com.messenger.loadtest.creators.ChatsCreator;
import com.messenger.loadtest.creators.MessagesCreator;
import com.messenger.loadtest.creators.UserCreator;
import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class WriteLoop implements ApplicationRunner {
    public static volatile boolean STOP;

    private final UserCreator userCreator;
    private final ChatsCreator chatsCreator;
    private final MessagesCreator messagesCreator;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public WriteLoop(
            UserCreator userCreator,
            ChatsCreator chatsCreator,
            MessagesCreator messagesCreator,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.userCreator = userCreator;
        this.chatsCreator = chatsCreator;
        this.messagesCreator = messagesCreator;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread thread = new Thread(this::loop, "loadtest-writer");
        thread.start();
    }

    private void loop() {
        while (!STOP) {
            userCreator.createUsersInDB(LoadtestApplication.COUNT_USERS);
            chatsCreator.createChatsAndUsersChats();
            messagesCreator.createMessagesInDB();
            printTableCounts();
        }
    }

    private void printTableCounts() {
        transactionTemplate.executeWithoutResult(status -> {
            System.out.println("Записей в БД:");
            printStatRow("Таблица", "Кол-во", "");
            printTableStat("users", "id");
            printTableStat("chats", "id");
            printUsersChatsStat();
            printTableStat("messages", "id");
            System.out.flush();
        });
    }

    private void printStatRow(String table, String count, String idStats) {
        System.out.printf("%-15s\t%12s\t%s%n", table, count, idStats);
    }

    private void printTableStat(String tableName, String idColumn) {
        long count = countRows(tableName);
        Long minId = minColumn(tableName, idColumn);
        Long maxId = maxColumn(tableName, idColumn);
        String idStats = String.format("(id min=%s, max=%s)", formatId(minId), formatId(maxId));
        printStatRow(tableName, String.valueOf(count), idStats);
    }

    private void printUsersChatsStat() {
        long count = countRows("users_chats");
        Long minUserId = minColumn("users_chats", "user_id");
        Long maxUserId = maxColumn("users_chats", "user_id");
        Long minChatId = minColumn("users_chats", "chat_id");
        Long maxChatId = maxColumn("users_chats", "chat_id");
        String idStats = String.format(
                "(user_id min=%s, max=%s; chat_id min=%s, max=%s)",
                formatId(minUserId), formatId(maxUserId), formatId(minChatId), formatId(maxChatId)
        );
        printStatRow("users_chats", String.valueOf(count), idStats);
    }

    private static String formatId(Long id) {
        return id == null ? "-" : id.toString();
    }

    private long countRows(String tableName) {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + tableName).getSingleResult()).longValue();
    }

    private Long minColumn(String tableName, String columnName) {
        Object result = entityManager.createNativeQuery("SELECT MIN(" + columnName + ") FROM " + tableName).getSingleResult();
        return toLongOrNull(result);
    }

    private Long maxColumn(String tableName, String columnName) {
        Object result = entityManager.createNativeQuery("SELECT MAX(" + columnName + ") FROM " + tableName).getSingleResult();
        return toLongOrNull(result);
    }

    private static Long toLongOrNull(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
