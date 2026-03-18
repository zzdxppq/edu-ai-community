package com.eduai.community.controller;

import com.eduai.community.model.dto.EscalateRequest;
import com.eduai.community.model.dto.FeedbackRequest;
import com.eduai.community.service.FeedbackService;
import com.eduai.community.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ApiResponse<Map<String, Object>> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        try {
            Map<String, Object> result = feedbackService.submitFeedback(userId, request);
            return ApiResponse.success("反馈提交成功", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/escalate")
    public ApiResponse<Map<String, Object>> escalate(
            @Valid @RequestBody EscalateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        try {
            Map<String, Object> result = feedbackService.escalate(userId, request.getMessageId());
            return ApiResponse.success("已转交运营人员处理", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{messageId}")
    public ApiResponse<Map<String, Object>> getFeedback(@PathVariable Long messageId) {
        Map<String, Object> result = feedbackService.getFeedback(messageId);
        if (result == null) {
            return ApiResponse.success("暂无反馈", null);
        }
        return ApiResponse.success(result);
    }
}
