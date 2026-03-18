package com.eduai.community.repository;

import com.eduai.community.model.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
