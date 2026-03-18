package com.eduai.community.controller;

import com.eduai.community.model.dto.ChatRequest;
import com.eduai.community.service.ChatService;
import com.eduai.community.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        try {
            Map<String, Object> result = chatService.chat(userId, request.getConversationId(), request.getMessage());
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/conversations")
    public ApiResponse<List<Map<String, Object>>> listConversations(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<Map<String, Object>> result = chatService.listConversations(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<Map<String, Object>>> getMessages(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        try {
            List<Map<String, Object>> result = chatService.getMessages(userId, id);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/chat/quick-questions")
    public ApiResponse<List<Map<String, Object>>> getQuickQuestions() {
        return ApiResponse.success(chatService.getQuickQuestions());
    }
}
