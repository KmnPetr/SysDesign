package com.messenger.loadtest.controller;

import com.messenger.loadtest.models.User;
import com.messenger.loadtest.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/users")
public class UserController {
    public static volatile long MIN_USER_ID;
    public static volatile long MAX_USER_ID;

    private final UserService userService;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public UserController(
            UserService userService,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.userService = userService;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    public void initUserIdBounds() {
        refreshUserIdBounds();
    }

    public void refreshUserIdBounds() {
        transactionTemplate.executeWithoutResult(status -> {
            Object[] row = (Object[]) entityManager.createNativeQuery(
                    "SELECT MIN(id), MAX(id) FROM users"
            ).getSingleResult();
            MIN_USER_ID = toLongOrZero(row[0]);
            MAX_USER_ID = toLongOrZero(row[1]);
        });
    }

    private static long toLongOrZero(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    @GetMapping("/random")
    public ResponseEntity<User> getRandomUser() {
        if (MAX_USER_ID == 0L) {
            return ResponseEntity.notFound().build();
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            long randomId = ThreadLocalRandom.current().nextLong(MIN_USER_ID, MAX_USER_ID + 1);
            Optional<User> user = userService.getUserById(randomId);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
