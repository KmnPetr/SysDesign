package com.messenger.loadtest.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.loadtest.dto.ChatMessagesResponse;
import com.messenger.loadtest.models.Chat;
import com.messenger.loadtest.models.Message;
import com.messenger.loadtest.models.User;
import com.messenger.loadtest.models.UserChat;
import com.messenger.loadtest.repositories.ChatRepository;
import com.messenger.loadtest.repositories.MessageRepository;
import com.messenger.loadtest.repositories.UserChatRepository;
import com.messenger.loadtest.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MessageService {
    private static final String MESSAGES_RESOURCE = "examples/messages.json";

    private final List<String> exampleMessages = new ArrayList<>();
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserChatRepository userChatRepository;
    private final MessageRepository messageRepository;

    public MessageService(
            ChatRepository chatRepository,
            UserRepository userRepository,
            UserChatRepository userChatRepository,
            MessageRepository messageRepository
    ) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.userChatRepository = userChatRepository;
        this.messageRepository = messageRepository;
    }

    @PostConstruct
    private void loadExampleMessages() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(MESSAGES_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Resource not found: " + MESSAGES_RESOURCE);
            }
            exampleMessages.addAll(mapper.readValue(input, new TypeReference<List<String>>() {}));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read messages from " + MESSAGES_RESOURCE, e);
        }
    }

    public Optional<ChatMessagesResponse> getChatWithMessages(Long chatId) {
        return chatRepository.findById(chatId)
                .map(this::buildChatMessagesResponse);
    }

    public Optional<Message> createMessage(Long chatId) {
        Message message = new Message();
        message.setChatId(chatId);
        message.setText(randomExampleText());
        try {
            return Optional.of(messageRepository.save(message));
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }

    private String randomExampleText() {
        return exampleMessages.get(ThreadLocalRandom.current().nextInt(exampleMessages.size()));
    }

    private ChatMessagesResponse buildChatMessagesResponse(Chat chat) {
        Long chatId = chat.getId();
        List<UserChat> userChats = userChatRepository.findByIdChatId(chatId);
        List<Long> userIds = userChats.stream()
                .map(userChat -> userChat.getId().getUserId())
                .toList();
        List<User> users = userIds.isEmpty()
                ? List.of()
                : new ArrayList<>(userRepository.findAllById(userIds));
        List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        return new ChatMessagesResponse(chat, users, userChats, messages);
    }
}
