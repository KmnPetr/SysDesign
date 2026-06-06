package com.messenger.loadtest.creators;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.loadtest.LoadtestApplication;
import com.messenger.loadtest.models.Message;
import com.messenger.loadtest.repositories.MessageRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MessagesCreator {
    private static final String MESSAGES_RESOURCE = "examples/messages.json";
    private static final int PARALLEL_BATCH_COUNT = 10;
    private static final List<String> exampleMessages = new ArrayList<>();
    private static int totalRandomCount;

    private final MessageRepository messageRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final LinkedList<ChatMsg> chatMsgs = new LinkedList<>();

    public MessagesCreator(
            MessageRepository messageRepository,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.messageRepository = messageRepository;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void createMessagesInDB() {
        loadExampleMessages();
        buildChatMsgs();

        totalRandomCount = 0;
        for (ChatMsg chatMsg : chatMsgs) {
            totalRandomCount += chatMsg.randomCount;
        }

        List<Message> batch = new ArrayList<>(LoadtestApplication.BATCH_SIZE);
        List<List<Message>> pendingBatches = new ArrayList<>(PARALLEL_BATCH_COUNT);
        int saved = 0;

        while (true) {
            if (chatMsgs.isEmpty()) break;

            Iterator<ChatMsg> iterator = chatMsgs.iterator();
            while (iterator.hasNext()) {
                ChatMsg chatMsg = iterator.next();
                if (ThreadLocalRandom.current().nextInt(20) != 0) {
                    continue;
                }

                String text = exampleMessages.get(chatMsg.lastIStr % exampleMessages.size());

                Message message = new Message();
                message.setChatId(chatMsg.chatId);
                message.setUserId(null);
                message.setText(text);
                batch.add(message);

                chatMsg.lastIStr++;
                if (chatMsg.lastIStr == chatMsg.randomCount) {
                    iterator.remove();
                }

                if (batch.size() == LoadtestApplication.BATCH_SIZE) {
                    pendingBatches.add(new ArrayList<>(batch));
                    batch.clear();
                    if (pendingBatches.size() == PARALLEL_BATCH_COUNT) {
                        saved += saveBatchesParallel(pendingBatches);
                        pendingBatches.clear();
                        printProgress(saved, totalRandomCount);
                    }
                }
            }
        }

        if (!batch.isEmpty()) {
            pendingBatches.add(new ArrayList<>(batch));
        }
        if (!pendingBatches.isEmpty()) {
            saved += saveBatchesParallel(pendingBatches);
            printProgress(saved, totalRandomCount);
        }
    }

    private int saveBatchesParallel(List<List<Message>> batches) {
        int batchCount = batches.size();
        CountDownLatch latch = new CountDownLatch(batchCount);
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        Thread[] threads = new Thread[batchCount];

        for (int i = 0; i < batchCount; i++) {
            List<Message> batch = batches.get(i);
            threads[i] = new Thread(() -> {
                try {
                    saveBatch(batch);
                } catch (RuntimeException e) {
                    error.compareAndSet(null, e);
                } finally {
                    latch.countDown();
                }
            }, "message-batch-writer-" + i);
            threads[i].start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while saving message batches", e);
        }

        if (error.get() != null) {
            throw error.get();
        }

        return batches.stream().mapToInt(List::size).sum();
    }

    private void saveBatch(List<Message> batch) {
        transactionTemplate.executeWithoutResult(status -> {
            messageRepository.saveAll(batch);
            entityManager.flush();
            entityManager.clear();
        });
    }

    private void printProgress(int saved, int total) {
        double percent = saved * 100.0 / total;
        System.out.printf("Сообщения: %.2f%% (%d / %d)%n", percent, saved, total);
        System.out.flush();
    }

    private void loadExampleMessages() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(MESSAGES_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Resource not found: " + MESSAGES_RESOURCE);
            }
            exampleMessages.clear();
            exampleMessages.addAll(mapper.readValue(input, new TypeReference<List<String>>() {}));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read messages from " + MESSAGES_RESOURCE, e);
        }
    }

    private void buildChatMsgs() {
        chatMsgs.clear();
        for (Long chatId : ChatsCreator.savedChatIds) {
            int randomCount = ThreadLocalRandom.current().nextInt(LoadtestApplication.MIN_COUNT_MSG, LoadtestApplication.MAX_COUNT_MSG + 1);
            if (ThreadLocalRandom.current().nextBoolean()) {
                randomCount = ThreadLocalRandom.current().nextInt(1, 6);
            }
            chatMsgs.add(new ChatMsg(chatId, randomCount, 0));
        }
    }

    public static class ChatMsg {
        public long chatId;
        public int randomCount;
        public int lastIStr;

        public ChatMsg(long chatId, int randomCount, int lastIStr) {
            this.chatId = chatId;
            this.randomCount = randomCount;
            this.lastIStr = lastIStr;
        }
    }
}
