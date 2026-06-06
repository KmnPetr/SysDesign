package com.messenger.loadtest.creators;

import com.messenger.loadtest.LoadtestApplication;
import com.messenger.loadtest.models.Chat;
import com.messenger.loadtest.models.User;
import com.messenger.loadtest.models.UserChat;
import com.messenger.loadtest.repositories.ChatRepository;
import com.messenger.loadtest.repositories.UserChatRepository;
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
    public static final List<Long> savedChatIds = new ArrayList<>();
    private static int nextChatIdIndex = 0;

    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public ChatsCreator(
            ChatRepository chatRepository,
            UserChatRepository userChatRepository,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.chatRepository = chatRepository;
        this.userChatRepository = userChatRepository;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void createChatsAndUsersChats() {
        createShaffleChatsIds(UserCreator.shaffleUsers.size());
        savedChatIds.clear();

        List<Chat> chatBatch = new ArrayList<>(LoadtestApplication.BATCH_SIZE);
        List<UserChat> userChatBatch = new ArrayList<>(LoadtestApplication.BATCH_SIZE);

        for (int i = 0; i < UserCreator.shaffleUsers.size(); i++) {
            User user = UserCreator.shaffleUsers.get(i);

            int countChats = ThreadLocalRandom.current().nextInt(LoadtestApplication.MIN_OWN_CHAT, LoadtestApplication.MAX_OWN_CHAT + 1);

            for (int nextUserIndex = i + 1; nextUserIndex <= i + countChats; nextUserIndex++) {
                if (nextUserIndex >= UserCreator.shaffleUsers.size()) break;

                User nextUser = UserCreator.shaffleUsers.get(nextUserIndex);
                Chat chat = createPrivateChat(user, nextUser);
                chatBatch.add(chat);
                addUserChats(user, nextUser, chat.getId(), userChatBatch);

                if (chatBatch.size() == LoadtestApplication.BATCH_SIZE) {
                    saveChatBatch(chatBatch, i);
                    chatBatch.clear();
                }
                flushUserChatBatchIfFull(userChatBatch, chatBatch, i);
            }
        }

        if (!chatBatch.isEmpty()) {
            saveChatBatch(chatBatch, UserCreator.shaffleUsers.size() - 1);
            chatBatch.clear();
        }
        if (!userChatBatch.isEmpty()) {
            saveUserChatBatch(userChatBatch);
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

    private void addUserChats(User user, User nextUser, Long chatId, List<UserChat> userChatBatch) {
        userChatBatch.add(new UserChat(new UserChat.Id(user.getId(), chatId), null, null, null));
        userChatBatch.add(new UserChat(new UserChat.Id(nextUser.getId(), chatId), null, null, null));
    }

    private void flushUserChatBatchIfFull(List<UserChat> userChatBatch, List<Chat> chatBatch, int currentUserIndex) {
        while (userChatBatch.size() >= LoadtestApplication.BATCH_SIZE) {
            savePendingChatBatch(chatBatch, currentUserIndex);
            List<UserChat> batch = new ArrayList<>(userChatBatch.subList(0, LoadtestApplication.BATCH_SIZE));
            saveUserChatBatch(batch);
            userChatBatch.subList(0, LoadtestApplication.BATCH_SIZE).clear();
        }
    }

    private void savePendingChatBatch(List<Chat> chatBatch, int currentUserIndex) {
        if (!chatBatch.isEmpty()) {
            saveChatBatch(chatBatch, currentUserIndex);
            chatBatch.clear();
        }
    }

    private void saveChatBatch(List<Chat> chatBatch, int currentUserIndex) {
        transactionTemplate.executeWithoutResult(status -> {
            chatRepository.saveAll(chatBatch);
            entityManager.flush();
            entityManager.clear();
        });
        for (Chat chat : chatBatch) {
            savedChatIds.add(chat.getId());
        }
        printProgress("Chats", currentUserIndex + 1, UserCreator.shaffleUsers.size());
    }

    private void saveUserChatBatch(List<UserChat> userChatBatch) {
        transactionTemplate.executeWithoutResult(status -> {
            userChatRepository.saveAll(userChatBatch);
            entityManager.flush();
            entityManager.clear();
        });
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
