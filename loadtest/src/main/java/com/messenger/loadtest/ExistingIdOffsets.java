package com.messenger.loadtest;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ExistingIdOffsets {
    private final long maxUserId;
    private final long maxChatId;

    public ExistingIdOffsets(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Long usersMax = transactionTemplate.execute(status -> queryMaxId(entityManager, "users", "id"));
        Long chatsMax = transactionTemplate.execute(status -> queryMaxId(entityManager, "chats", "id"));
        this.maxUserId = usersMax == null ? 0L : usersMax;
        this.maxChatId = chatsMax == null ? 0L : chatsMax;
        System.out.printf("Существующие max id: users=%d, chats=%d%n", maxUserId, maxChatId);
        System.out.flush();
    }

    public long getMaxUserId() {
        return maxUserId;
    }

    public long getMaxChatId() {
        return maxChatId;
    }

    private static Long queryMaxId(EntityManager entityManager, String tableName, String columnName) {
        Object result = entityManager.createNativeQuery(
                "SELECT MAX(" + columnName + ") FROM " + tableName
        ).getSingleResult();
        return result == null ? null : ((Number) result).longValue();
    }
}
