package com.playmotech.ghostcoach.repository;

import com.playmotech.ghostcoach.entity.ChatMessage;
import com.playmotech.ghostcoach.entity.CoachingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionOrderByCreatedAtAsc(CoachingSession session);

    void deleteBySession(CoachingSession session);
}
