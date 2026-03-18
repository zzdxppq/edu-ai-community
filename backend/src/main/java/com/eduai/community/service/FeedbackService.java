package com.eduai.community.service;

import com.eduai.community.model.dto.FeedbackRequest;
import com.eduai.community.model.entity.Feedback;
import com.eduai.community.model.entity.Message;
import com.eduai.community.repository.FeedbackRepository;
import com.eduai.community.repository.MessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> submitFeedback(Long userId, FeedbackRequest request) {
        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));

        Optional<Feedback> existing = feedbackRepository.findByMessageId(request.getMessageId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("该消息已提交过反馈");
        }

        String reasonsJson;
        try {
            reasonsJson = request.getReasons() != null
                    ? objectMapper.writeValueAsString(request.getReasons())
                    : "[]";
        } catch (JsonProcessingException e) {
            reasonsJson = "[]";
        }

        Feedback feedback = Feedback.builder()
                .messageId(request.getMessageId())
                .userId(userId)
                .rating(request.getRating())
                .reasons(reasonsJson)
                .comment(request.getComment())
                .escalated(false)
                .build();
        feedback = feedbackRepository.save(feedback);

        return buildFeedbackResponse(feedback);
    }

    @Transactional
    public Map<String, Object> escalate(Long userId, Long messageId) {
        Optional<Feedback> existing = feedbackRepository.findByMessageId(messageId);

        Feedback feedback;
        if (existing.isPresent()) {
            feedback = existing.get();
        } else {
            feedback = Feedback.builder()
                    .messageId(messageId)
                    .userId(userId)
                    .rating(1)
                    .reasons("[]")
                    .escalated(false)
                    .build();
        }

        feedback.setEscalated(true);
        feedback.setEscalateStatus(Feedback.EscalateStatus.PENDING.name());
        feedback = feedbackRepository.save(feedback);

        log.info("Feedback escalated: messageId={}, userId={}, feedbackId={}", messageId, userId, feedback.getId());

        return buildFeedbackResponse(feedback);
    }

    public Map<String, Object> getFeedback(Long messageId) {
        Feedback feedback = feedbackRepository.findByMessageId(messageId)
                .orElse(null);

        if (feedback == null) {
            return null;
        }

        return buildFeedbackResponse(feedback);
    }

    private Map<String, Object> buildFeedbackResponse(Feedback feedback) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", feedback.getId());
        result.put("messageId", feedback.getMessageId());
        result.put("userId", feedback.getUserId());
        result.put("rating", feedback.getRating());
        result.put("escalated", feedback.getEscalated());
        result.put("escalateStatus", feedback.getEscalateStatus());
        result.put("comment", feedback.getComment());
        result.put("createdAt", feedback.getCreatedAt());

        try {
            List<?> reasons = objectMapper.readValue(
                    feedback.getReasons() != null ? feedback.getReasons() : "[]",
                    List.class
            );
            result.put("reasons", reasons);
        } catch (JsonProcessingException e) {
            result.put("reasons", List.of());
        }

        return result;
    }
}
