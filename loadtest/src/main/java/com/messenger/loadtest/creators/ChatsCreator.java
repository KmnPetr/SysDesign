package com.messenger.loadtest.creators;

import com.messenger.loadtest.ExistingIdOffsets;
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
import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ChatsCreator {
    private static final int PARALLEL_USER_CHAT_BATCH_COUNT = 10;
    private static final int PARALLEL_CHAT_BATCH_COUNT = 5;

    private static final List<Long> shaffleChatsIds = new ArrayList<>();
    public static final List<Long> savedChatIds = new ArrayList<>();
    private static int nextChatIdIndex = 0;

    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final ExistingIdOffsets existingIdOffsets;
    private int savedChatsCount;
    private int savedUserChatsCount;

    public ChatsCreator(
            ChatRepository chatRepository,
            UserChatRepository userChatRepository,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager,
            ExistingIdOffsets existingIdOffsets
    ) {
        this.chatRepository = chatRepository;
        this.userChatRepository = userChatRepository;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.existingIdOffsets = existingIdOffsets;
    }

    public void createChatsAndUsersChats() {
        createShaffleChatsIds(UserCreator.shaffleUsers.size());
        savedChatIds.clear();
        savedChatsCount = 0;
        savedUserChatsCount = 0;

        List<Chat> chatBatch = new ArrayList<>(LoadtestApplication.BATCH_SIZE);
        List<UserChat> userChatBatch = new ArrayList<>(LoadtestApplication.BATCH_SIZE);
        List<List<Chat>> pendingChatBatches = new ArrayList<>(PARALLEL_CHAT_BATCH_COUNT);
        List<List<UserChat>> pendingUserChatBatches = new ArrayList<>(PARALLEL_USER_CHAT_BATCH_COUNT);

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
                    pendingChatBatches.add(new ArrayList<>(chatBatch));
                    chatBatch.clear();
                }
                if (userChatBatch.size() == LoadtestApplication.BATCH_SIZE) {
                    pendingUserChatBatches.add(new ArrayList<>(userChatBatch));
                    userChatBatch.clear();
                }
                if (shouldFlush(pendingChatBatches, pendingUserChatBatches)) {
                    flushPendingBatches(pendingChatBatches, pendingUserChatBatches, i);
                }
            }
        }

        if (!chatBatch.isEmpty()) {
            pendingChatBatches.add(new ArrayList<>(chatBatch));
        }
        if (!userChatBatch.isEmpty()) {
            pendingUserChatBatches.add(new ArrayList<>(userChatBatch));
        }
        if (!pendingChatBatches.isEmpty() || !pendingUserChatBatches.isEmpty()) {
            flushPendingBatches(
                    pendingChatBatches,
                    pendingUserChatBatches,
                    UserCreator.shaffleUsers.size() - 1
            );
        }

        resetChatsIdSequence();
    }

    private boolean shouldFlush(List<List<Chat>> pendingChatBatches, List<List<UserChat>> pendingUserChatBatches) {
        return pendingUserChatBatches.size() >= PARALLEL_USER_CHAT_BATCH_COUNT
                || pendingChatBatches.size() >= PARALLEL_CHAT_BATCH_COUNT;
    }

    private void flushPendingBatches(
            List<List<Chat>> pendingChatBatches,
            List<List<UserChat>> pendingUserChatBatches,
            int currentUserIndex
    ) {
        if (!pendingChatBatches.isEmpty()) {
            savedChatsCount += countItems(pendingChatBatches);
            saveChatBatchesParallel(pendingChatBatches);
            for (List<Chat> batch : pendingChatBatches) {
                for (Chat chat : batch) {
                    savedChatIds.add(chat.getId());
                }
            }
            pendingChatBatches.clear();
        }
        if (!pendingUserChatBatches.isEmpty()) {
            savedUserChatsCount += countItems(pendingUserChatBatches);
            saveUserChatBatchesParallel(pendingUserChatBatches);
            pendingUserChatBatches.clear();
        }
        if (savedChatsCount > 0 || savedUserChatsCount > 0) {
            printProgress("Chat UserChat", currentUserIndex + 1, UserCreator.shaffleUsers.size());
        }
    }

    private <T> int countItems(List<List<T>> batches) {
        return batches.stream().mapToInt(List::size).sum();
    }

    private void saveChatBatchesParallel(List<List<Chat>> batches) {
        runBatchesParallel(batches, this::saveChatBatch);
    }

    private void saveUserChatBatchesParallel(List<List<UserChat>> batches) {
        runBatchesParallel(batches, this::saveUserChatBatch);
    }

    private <T> void runBatchesParallel(List<List<T>> batches, Consumer<List<T>> saver) {
        int batchCount = batches.size();
        CountDownLatch latch = new CountDownLatch(batchCount);
        AtomicReference<RuntimeException> error = new AtomicReference<>();

        for (int i = 0; i < batchCount; i++) {
            List<T> batch = batches.get(i);
            Thread thread = new Thread(() -> {
                try {
                    saver.accept(batch);
                } catch (RuntimeException e) {
                    error.compareAndSet(null, e);
                } finally {
                    latch.countDown();
                }
            }, "chat-batch-writer-" + i);
            thread.start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while saving chat batches", e);
        }

        if (error.get() != null) {
            throw error.get();
        }
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

    private void saveChatBatch(List<Chat> chatBatch) {
        transactionTemplate.executeWithoutResult(status -> {
            chatRepository.saveAll(chatBatch);
            entityManager.flush();
            entityManager.clear();
        });
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
        System.out.printf(
                "%s: %.2f%% (%d / %d) Chat=%d UserChat=%d%n",
                label, percent, processed, total, savedChatsCount, savedUserChatsCount
        );
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
        long startId = existingIdOffsets.getMaxChatId();
        for (long id = startId + 1; id <= startId + count; id++) {
            shaffleChatsIds.add(id);
        }
        Collections.shuffle(shaffleChatsIds);
    }
}
