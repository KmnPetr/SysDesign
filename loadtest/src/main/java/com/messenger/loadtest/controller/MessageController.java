package com.messenger.loadtest.controller;

import com.messenger.loadtest.dto.ChatMessagesResponse;
import com.messenger.loadtest.models.Message;
import com.messenger.loadtest.service.MessageService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    public static volatile long MIN_CHAT_ID;
    public static volatile long MAX_CHAT_ID;

    private final MessageService messageService;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public MessageController(
            MessageService messageService,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.messageService = messageService;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    public void initChatIdBounds() {
        refreshChatIdBounds();
    }

    public void refreshChatIdBounds() {
        transactionTemplate.executeWithoutResult(status -> {
            Object[] row = (Object[]) entityManager.createNativeQuery(
                    "SELECT MIN(id), MAX(id) FROM chats"
            ).getSingleResult();
            MIN_CHAT_ID = toLongOrZero(row[0]);
            MAX_CHAT_ID = toLongOrZero(row[1]);
        });
    }

    private static long toLongOrZero(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    @GetMapping("/random")
    public ResponseEntity<ChatMessagesResponse> getRandomMessages() {
        if (MAX_CHAT_ID == 0L) {
            return ResponseEntity.notFound().build();
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            long randomChatId = ThreadLocalRandom.current().nextLong(MIN_CHAT_ID, MAX_CHAT_ID + 1);
            var response = messageService.getChatWithMessages(randomChatId);
            if (response.isPresent()) {
                return ResponseEntity.ok(response.get());
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{chat_id}")
    public ResponseEntity<ChatMessagesResponse> getMessages(@PathVariable("chat_id") Long chatId) {
        return messageService.getChatWithMessages(chatId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{chat_id}")
    public ResponseEntity<Message> createMessage(@PathVariable("chat_id") Long chatId) {
        return messageService.createMessage(chatId)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.badRequest().build());
    }
}
