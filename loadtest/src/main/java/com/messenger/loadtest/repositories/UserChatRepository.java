package com.messenger.loadtest.repositories;

import com.messenger.loadtest.models.UserChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserChatRepository extends JpaRepository<UserChat, UserChat.Id> {
}
