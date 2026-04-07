package com.bishe.recruitment.config;

import com.bishe.recruitment.security.JwtUtils;
import java.util.List;
import java.util.Objects;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;

    public WebSocketAuthChannelInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                accessor.setUser(new StompPrincipal(String.valueOf(jwtUtils.getUserIdFromToken(token))));
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String value = authorization.getFirst();
            if (StringUtils.hasText(value) && value.startsWith("Bearer ")) {
                return value.substring(7);
            }
            return value;
        }
        List<String> tokenHeader = accessor.getNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            return tokenHeader.getFirst();
        }
        Object token = accessor.getSessionAttributes() == null ? null : accessor.getSessionAttributes().get("token");
        return Objects.toString(token, null);
    }
}
