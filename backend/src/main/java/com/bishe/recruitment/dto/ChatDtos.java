package com.bishe.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public final class ChatDtos {

    private ChatDtos() {
    }

    @Data
    public static class SendChatMessageRequest {
        @NotNull(message = "接收人不能为空")
        private Long receiverId;

        @NotBlank(message = "消息内容不能为空")
        private String content;
    }
}
