package com.messenger.loadtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateMessageRequest {
    @JsonProperty("user_id")
    private Long userId;
    private String text;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
