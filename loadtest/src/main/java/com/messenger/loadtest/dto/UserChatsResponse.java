package com.messenger.loadtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.messenger.loadtest.models.Chat;
import com.messenger.loadtest.models.User;
import com.messenger.loadtest.models.UserChat;

import java.util.List;

public class UserChatsResponse {
    private User user;
    private List<Chat> chats;
    @JsonProperty("user_chats")
    private List<UserChat> userChats;

    public UserChatsResponse(User user, List<Chat> chats, List<UserChat> userChats) {
        this.user = user;
        this.chats = chats;
        this.userChats = userChats;
    }

    public User getUser() {
        return user;
    }

    public List<Chat> getChats() {
        return chats;
    }

    public List<UserChat> getUserChats() {
        return userChats;
    }
}
