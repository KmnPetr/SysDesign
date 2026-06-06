package com.messenger.loadtest.service;

import com.messenger.loadtest.dto.UserChatsResponse;
import com.messenger.loadtest.models.Chat;
import com.messenger.loadtest.models.User;
import com.messenger.loadtest.models.UserChat;
import com.messenger.loadtest.repositories.ChatRepository;
import com.messenger.loadtest.repositories.UserChatRepository;
import com.messenger.loadtest.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;

    public UserService(
            UserRepository userRepository,
            ChatRepository chatRepository,
            UserChatRepository userChatRepository
    ) {
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.userChatRepository = userChatRepository;
    }

    public Optional<UserChatsResponse> getUserWithChats(Long id) {
        return userRepository.findById(id)
                .map(this::buildUserResponse);
    }

    private UserChatsResponse buildUserResponse(User user) {
        List<UserChat> userChats = userChatRepository.findByIdUserId(user.getId());
        List<Long> chatIds = userChats.stream()
                .map(userChat -> userChat.getId().getChatId())
                .toList();
        List<Chat> chats = chatIds.isEmpty()
                ? List.of()
                : new ArrayList<>(chatRepository.findAllById(chatIds));
        return new UserChatsResponse(user, chats, userChats);
    }
}
