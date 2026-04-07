package com.bishe.recruitment.controller;

import com.bishe.recruitment.common.ApiResponse;
import com.bishe.recruitment.dto.ChatDtos;
import com.bishe.recruitment.service.MessageService;
import com.bishe.recruitment.service.NotificationService;
import com.bishe.recruitment.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final MessageService messageService;
    private final NotificationService notificationService;

    public MessageController(MessageService messageService, NotificationService notificationService) {
        this.messageService = messageService;
        this.notificationService = notificationService;
    }

    @GetMapping("/messages/conversations")
    public ApiResponse<?> conversations() {
        return ApiResponse.success(messageService.listConversations(SecurityUtils.currentUserId()));
    }

    @GetMapping("/messages/conversations/{conversationId}")
    public ApiResponse<?> messages(@PathVariable Long conversationId) {
        return ApiResponse.success(messageService.listMessages(SecurityUtils.currentUserId(), conversationId));
    }

    @PostMapping("/messages/conversations/{peerUserId}")
    public ApiResponse<?> ensureConversation(@PathVariable Long peerUserId) {
        return ApiResponse.success(messageService.ensureConversation(SecurityUtils.currentUserId(), peerUserId));
    }

    @PostMapping("/messages/conversations/{conversationId}/messages")
    public ApiResponse<?> sendMessage(@PathVariable Long conversationId, @Valid @RequestBody ChatDtos.SendChatMessageRequest request) {
        return ApiResponse.success(
                "消息发送成功",
                messageService.sendMessageInConversation(SecurityUtils.currentUserId(), conversationId, request.getContent()));
    }

    @GetMapping("/notifications")
    public ApiResponse<?> notifications() {
        return ApiResponse.success(notificationService.listByUserId(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ApiResponse<?> read(@PathVariable Long notificationId) {
        notificationService.markRead(SecurityUtils.currentUserId(), notificationId);
        return ApiResponse.success("通知已读", null);
    }

    @PatchMapping("/notifications/read-all")
    public ApiResponse<?> readAll() {
        notificationService.markAllRead(SecurityUtils.currentUserId());
        return ApiResponse.success("全部通知已读", null);
    }
}
