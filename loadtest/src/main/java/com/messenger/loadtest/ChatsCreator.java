package com.messenger.loadtest;

import com.messenger.loadtest.models.Chat;
import com.messenger.loadtest.models.User;
import com.messenger.loadtest.repositories.ChatRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ChatsCreator {
    private static final List<Long> shaffleChatsIds = new ArrayList<>();
    private static int nextChatIdIndex = 0;

    private final ChatRepository chatRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public ChatsCreator(
            ChatRepository chatRepository,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.chatRepository = chatRepository;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void createChatsAndUsersChats() {
        createShaffleChatsIds(UserCreator.shaffleUsers.size());

        List<Chat> batch = new ArrayList<>(LoadtestApplication.BATCH_SIZE);

        for (int i = 0; i < UserCreator.shaffleUsers.size(); i++) {
            User user = UserCreator.shaffleUsers.get(i);

            int countChats = ThreadLocalRandom.current().nextInt(
                    LoadtestApplication.MIN_OWN_CHAT,
                    LoadtestApplication.MAX_OWN_CHAT + 1
            );

            for (int nextUserIndex = i + 1; nextUserIndex <= i + countChats; nextUserIndex++) {
                if (nextUserIndex >= UserCreator.shaffleUsers.size()) break;

                User nextUser = UserCreator.shaffleUsers.get(nextUserIndex);
                batch.add(createPrivateChat(user, nextUser));

                if (batch.size() == LoadtestApplication.BATCH_SIZE) {
                    saveBatch(batch, i);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            saveBatch(batch, UserCreator.shaffleUsers.size() - 1);
        }

        resetChatsIdSequence();
    }

    private Chat createPrivateChat(User user, User nextUser) {
        Chat chat = new Chat();
        chat.setId(getNextChatId());
        chat.setType("private");
        chat.setTitle(formatChatTitle(user, nextUser));
        return chat;
    }

    private String formatChatTitle(User user, User nextUser) {
        String firstName1 = user.getUsername().split(" ", 2)[0];
        String firstName2 = nextUser.getUsername().split(" ", 2)[0];
        return firstName1 + " × " + firstName2;
    }

    private void saveBatch(List<Chat> batch, int currentUserIndex) {
        transactionTemplate.executeWithoutResult(status -> {
            chatRepository.saveAll(batch);
            entityManager.flush();
            entityManager.clear();
        });
        printProgress("Создано", currentUserIndex + 1, UserCreator.shaffleUsers.size());
    }

    private void printProgress(String label, int processed, int total) {
        double percent = processed * 100.0 / total;
        System.out.printf("%s: %.2f%% (%d / %d)%n", label, percent, processed, total);
        System.out.flush();
    }

    private void resetChatsIdSequence() {
        transactionTemplate.executeWithoutResult(status ->
                entityManager.createNativeQuery(
                        "SELECT setval(pg_get_serial_sequence('chats', 'id'), COALESCE((SELECT MAX(id) FROM chats), 1))"
                ).getSingleResult()
        );
    }

    public Long getNextChatId() {
        if (nextChatIdIndex >= shaffleChatsIds.size()) {
            throw new IllegalStateException("No more chat ids in shaffleChatsIds");
        }
        return shaffleChatsIds.get(nextChatIdIndex++);
    }

    private void createShaffleChatsIds(int countCreatedUsers) {
        double avgChatsPerUser = (LoadtestApplication.MIN_OWN_CHAT + LoadtestApplication.MAX_OWN_CHAT) / 2.0;
        int count = (int) Math.ceil(countCreatedUsers * avgChatsPerUser * 1.5);
        shaffleChatsIds.clear();
        nextChatIdIndex = 0;
        for (long id = 1; id <= count; id++) {
            shaffleChatsIds.add(id);
        }
        Collections.shuffle(shaffleChatsIds);
    }
}
