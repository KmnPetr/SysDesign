package com.messenger.loadtest.controller;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InfoController {
    private static final List<String> ENDPOINTS = List.of(
            "GET http://localhost:4200/api/info",
            "GET http://localhost:4200/api/users/{id}",
            "GET http://localhost:4200/api/users/random",
            "GET http://localhost:4200/api/messages/{chat_id}",
            "POST http://localhost:4200/api/messages/{chat_id}",
            "GET http://localhost:4200/api/messages/random",
            "GET http://localhost:4200/api/write/stop"
    );

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public InfoController(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        return transactionTemplate.execute(status -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("users", formatNumber(countRows("users")));
            info.put("chats", formatNumber(countRows("chats")));
            info.put("users_chats", formatNumber(countRows("users_chats")));
            info.put("messages", formatNumber(countRows("messages")));
            info.put("endpoints", ENDPOINTS);
            return info;
        });
    }

    private static String formatNumber(long value) {
        return String.format(Locale.US, "%,d", value).replace(',', '_');
    }

    private long countRows(String tableName) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM " + tableName
        ).getSingleResult()).longValue();
    }
}
