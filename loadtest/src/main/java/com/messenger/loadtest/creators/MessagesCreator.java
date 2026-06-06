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
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MessagesCreator {
    private static final String MESSAGES_RESOURCE = "examples/messages.json";
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
                    saveBatch(batch);
                    saved += batch.size();
                    batch.clear();
                    printProgress(saved, totalRandomCount);
                }
            }
        }

        if (!batch.isEmpty()) {
            saveBatch(batch);
            saved += batch.size();
            printProgress(saved, totalRandomCount);
        }
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
