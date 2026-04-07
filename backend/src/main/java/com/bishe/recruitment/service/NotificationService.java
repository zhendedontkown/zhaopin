package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.entity.Notification;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.mapper.NotificationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public NotificationService(NotificationMapper notificationMapper, SimpMessagingTemplate simpMessagingTemplate) {
        this.notificationMapper = notificationMapper;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public Notification createAndPush(Long userId, NotificationType type, String title, String content) {
        return createAndPush(userId, type, title, content, null, null);
    }

    public Notification createAndPush(Long userId, NotificationType type, String title, String content,
                                      Long relatedUserId, Long relatedConversationId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type.name());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(0);
        notification.setRelatedUserId(relatedUserId);
        notification.setRelatedConversationId(relatedConversationId);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", notification);
        return notification;
    }

    public List<Notification> listByUserId(Long userId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt));
    }

    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException("\u901a\u77e5\u4e0d\u5b58\u5728");
        }
        notification.setReadFlag(1);
        notificationMapper.updateById(notification);
    }

    public void markAllRead(Long userId) {
        notificationMapper.update(
                null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getReadFlag, 0)
                        .set(Notification::getReadFlag, 1)
        );
    }
}
