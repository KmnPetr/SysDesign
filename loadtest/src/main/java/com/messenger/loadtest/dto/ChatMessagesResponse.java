package com.messenger.loadtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.messenger.loadtest.models.Chat;
import com.messenger.loadtest.models.Message;
import com.messenger.loadtest.models.User;
import com.messenger.loadtest.models.UserChat;

import java.util.List;

public class ChatMessagesResponse {
    private Chat chat;
    private List<User> users;
    @JsonProperty("user_chats")
    private List<UserChat> userChats;
    private List<Message> messages;

    public ChatMessagesResponse(Chat chat, List<User> users, List<UserChat> userChats, List<Message> messages) {
        this.chat = chat;
        this.users = users;
        this.userChats = userChats;
        this.messages = messages;
    }

    public Chat getChat() {
        return chat;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<UserChat> getUserChats() {
        return userChats;
    }

    public List<Message> getMessages() {
        return messages;
    }
}
