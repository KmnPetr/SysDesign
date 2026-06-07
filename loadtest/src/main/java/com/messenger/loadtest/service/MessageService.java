package com.messenger.loadtest.service;

import com.messenger.loadtest.dto.ChatMessagesResponse;
import com.messenger.loadtest.models.Chat;
import com.messenger.loadtest.models.Message;
import com.messenger.loadtest.models.User;
import com.messenger.loadtest.models.UserChat;
import com.messenger.loadtest.repositories.ChatRepository;
import com.messenger.loadtest.repositories.MessageRepository;
import com.messenger.loadtest.repositories.UserChatRepository;
import com.messenger.loadtest.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {
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

    public Optional<ChatMessagesResponse> getChatWithMessages(Long chatId) {
        return chatRepository.findById(chatId)
                .map(this::buildChatMessagesResponse);
    }

    public Optional<Message> createMessage(Long chatId, Long userId, String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        if (!chatRepository.existsById(chatId)) {
            return Optional.empty();
        }
        boolean isMember = userChatRepository.findByIdChatId(chatId).stream()
                .anyMatch(userChat -> userId.equals(userChat.getId().getUserId()));
        if (!isMember) {
            return Optional.empty();
        }

        Message message = new Message();
        message.setChatId(chatId);
        message.setUserId(userId);
        message.setText(text);
        return Optional.of(messageRepository.save(message));
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
