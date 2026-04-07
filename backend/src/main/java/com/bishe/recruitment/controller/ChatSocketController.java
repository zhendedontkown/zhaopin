package com.bishe.recruitment.controller;

import com.bishe.recruitment.dto.ChatDtos;
import com.bishe.recruitment.service.MessageService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ChatSocketController {

    private final MessageService messageService;

    public ChatSocketController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/chat/send")
    public void send(@Valid @Payload ChatDtos.SendChatMessageRequest request, Principal principal) {
        Long currentUserId = Long.parseLong(principal.getName());
        messageService.sendMessage(currentUserId, request.getReceiverId(), request.getContent());
    }
}
