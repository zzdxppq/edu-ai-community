package com.eduai.community.repository;

import com.eduai.community.model.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByMessageId(Long messageId);
}
