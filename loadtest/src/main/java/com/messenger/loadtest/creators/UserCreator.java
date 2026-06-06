package com.messenger.loadtest.creators;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.loadtest.ExistingIdOffsets;
import com.messenger.loadtest.LoadtestApplication;
import com.messenger.loadtest.models.User;
import com.messenger.loadtest.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class UserCreator {
    private static final String USERS_RESOURCE = "examples/users.json";
    private static final List<Long> shaffleUsersIds = new ArrayList<>();
    public static final List<User> shaffleUsers = new ArrayList<>();

    private List<User> examplesUsers = new ArrayList<>();

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final ExistingIdOffsets existingIdOffsets;

    public UserCreator(
            UserRepository userRepository,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager,
            ExistingIdOffsets existingIdOffsets
    ) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.existingIdOffsets = existingIdOffsets;
    }

    public void createUsersInDB(int count) {
        existingIdOffsets.refresh();
        initExamples();
        prepareShuffledIds(count);
        shaffleUsers.clear();

        List<User> batch = new ArrayList<>(LoadtestApplication.BATCH_SIZE);
        int saved = 0;

        for (int i = 0; i < count; i++) {
            User user = getRandomUser();
            user.setId(shaffleUsersIds.get(i));
            batch.add(user);

            if (batch.size() == LoadtestApplication.BATCH_SIZE) {
                saveBatch(batch);
                saved += batch.size();
                batch.clear();
                printProgress("Users", saved, count);
            }
        }

        if (!batch.isEmpty()) {
            saveBatch(batch);
            saved += batch.size();
            printProgress("Users", saved, count);
        }

        resetUsersIdSequence();
    }

    private void initExamples() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(USERS_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Resource not found: " + USERS_RESOURCE);
            }
            examplesUsers = mapper.readValue(input, new TypeReference<List<User>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read users from " + USERS_RESOURCE, e);
        }
    }

    private User getRandomUser() {
        User source = examplesUsers.get(ThreadLocalRandom.current().nextInt(examplesUsers.size()));
        User user = new User();
        user.setUsername(source.getUsername());
        return user;
    }

    private void prepareShuffledIds(int count) {
        shaffleUsersIds.clear();
        long startId = existingIdOffsets.getMaxUserId();
        for (long id = startId + 1; id <= startId + count; id++) {
            shaffleUsersIds.add(id);
        }
        Collections.shuffle(shaffleUsersIds);
    }

    private void resetUsersIdSequence() {
        transactionTemplate.executeWithoutResult(status -> entityManager.createNativeQuery("SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1))").getSingleResult());
    }

    private void saveBatch(List<User> batch) {
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.saveAll(batch);
            entityManager.flush();
            entityManager.clear();
        });
        shaffleUsers.addAll(batch);
    }

    private void printProgress(String label, int saved, int total) {
        double percent = saved * 100.0 / total;
        System.out.printf("%s: %.2f%% (%d / %d)%n", label, percent, saved, total);
        System.out.flush();
    }
}
