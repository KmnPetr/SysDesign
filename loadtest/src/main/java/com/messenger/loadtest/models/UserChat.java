package com.messenger.loadtest.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "users_chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserChat {

    @EmbeddedId
    private Id id;

    @Column(length = 16)
    private String role;

    @Column(name = "is_banned")
    private Boolean isBanned;

    @Column(name = "last_read_msg_id")
    private Long lastReadMsgId;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Id implements Serializable {

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "chat_id")
        private Long chatId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id)) return false;

            Id other = (Id) o;

            return Objects.equals(userId, other.userId) && Objects.equals(chatId, other.chatId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, chatId);
        }
    }
}