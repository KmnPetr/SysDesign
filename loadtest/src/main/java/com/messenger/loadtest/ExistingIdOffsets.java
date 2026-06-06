package com.messenger.loadtest;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ExistingIdOffsets {
    private volatile long maxUserId;
    private volatile long maxChatId;

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public ExistingIdOffsets(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        refresh();
    }

    public void refresh() {
        transactionTemplate.executeWithoutResult(status -> {
            Long usersMax = queryMaxId("users", "id");
            Long chatsMax = queryMaxId("chats", "id");
            maxUserId = usersMax == null ? 0L : usersMax;
            maxChatId = chatsMax == null ? 0L : chatsMax;
        });
        System.out.printf("Существующие max id: users=%d, chats=%d%n", maxUserId, maxChatId);
        System.out.flush();
    }

    public long getMaxUserId() {
        return maxUserId;
    }

    public long getMaxChatId() {
        return maxChatId;
    }

    private Long queryMaxId(String tableName, String columnName) {
        Object result = entityManager.createNativeQuery(
                "SELECT MAX(" + columnName + ") FROM " + tableName
        ).getSingleResult();
        return result == null ? null : ((Number) result).longValue();
    }
}
