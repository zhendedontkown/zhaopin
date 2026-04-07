package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.entity.ChatMessage;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.Conversation;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.enums.NotificationType;
import com.bishe.recruitment.enums.UserRole;
import com.bishe.recruitment.mapper.ChatMessageMapper;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.ConversationMapper;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final ApplicationService applicationService;
    private final UserSupportService userSupportService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public MessageService(ConversationMapper conversationMapper, ChatMessageMapper chatMessageMapper,
                          CompanyProfileMapper companyProfileMapper, JobseekerProfileMapper jobseekerProfileMapper,
                          ApplicationService applicationService, UserSupportService userSupportService,
                          NotificationService notificationService, SimpMessagingTemplate simpMessagingTemplate) {
        this.conversationMapper = conversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.companyProfileMapper = companyProfileMapper;
        this.jobseekerProfileMapper = jobseekerProfileMapper;
        this.applicationService = applicationService;
        this.userSupportService = userSupportService;
        this.notificationService = notificationService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public List<Map<String, Object>> listConversations(Long currentUserId) {
        String role = userSupportService.getPrimaryRole(currentUserId);
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .orderByDesc(Conversation::getLastMessageAt);
        if (UserRole.COMPANY.name().equals(role)) {
            wrapper.eq(Conversation::getCompanyUserId, currentUserId);
        } else {
            wrapper.eq(Conversation::getJobseekerUserId, currentUserId);
        }
        return conversationMapper.selectList(wrapper).stream()
                .map(conversation -> toConversationView(conversation, currentUserId))
                .toList();
    }

    public List<Map<String, Object>> listMessages(Long currentUserId, Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        validateParticipant(currentUserId, conversation);
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreatedAt));
        for (ChatMessage message : messages) {
            if (currentUserId.equals(message.getReceiverUserId()) && message.getReadFlag() == 0) {
                message.setReadFlag(1);
                chatMessageMapper.updateById(message);
            }
        }
        return messages.stream().map(this::toMessageView).toList();
    }

    public Map<String, Object> ensureConversation(Long currentUserId, Long peerUserId) {
        String currentRole = userSupportService.getPrimaryRole(currentUserId);
        String peerRole = userSupportService.getPrimaryRole(peerUserId);
        if (!isCompanyAndJobseeker(currentRole, peerRole)) {
            throw new BusinessException("\u4ec5\u652f\u6301\u4f01\u4e1a\u4e0e\u6c42\u804c\u8005\u4e4b\u95f4\u5efa\u7acb\u4f1a\u8bdd");
        }

        Long companyUserId = UserRole.COMPANY.name().equals(currentRole) ? currentUserId : peerUserId;
        Long jobseekerUserId = UserRole.JOBSEEKER.name().equals(currentRole) ? currentUserId : peerUserId;
        ensureApplicationRelation(companyUserId, jobseekerUserId);

        Conversation conversation = getOrCreateConversation(companyUserId, jobseekerUserId);
        return toConversationView(conversation, currentUserId);
    }

    @Transactional
    public Map<String, Object> sendMessageInConversation(Long senderUserId, Long conversationId, String content) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        validateParticipant(senderUserId, conversation);
        Long receiverUserId = Objects.equals(senderUserId, conversation.getCompanyUserId())
                ? conversation.getJobseekerUserId()
                : conversation.getCompanyUserId();
        return persistAndDispatch(senderUserId, receiverUserId, conversation, content);
    }

    @Transactional
    public Map<String, Object> sendMessage(Long senderUserId, Long receiverUserId, String content) {
        String senderRole = userSupportService.getPrimaryRole(senderUserId);
        String receiverRole = userSupportService.getPrimaryRole(receiverUserId);
        if (!isCompanyAndJobseeker(senderRole, receiverRole)) {
            throw new BusinessException("\u4ec5\u652f\u6301\u4f01\u4e1a\u4e0e\u6c42\u804c\u8005\u4e4b\u95f4\u7684\u4e00\u5bf9\u4e00\u6c9f\u901a");
        }

        Long companyUserId = UserRole.COMPANY.name().equals(senderRole) ? senderUserId : receiverUserId;
        Long jobseekerUserId = UserRole.JOBSEEKER.name().equals(senderRole) ? senderUserId : receiverUserId;
        ensureApplicationRelation(companyUserId, jobseekerUserId);

        Conversation conversation = getOrCreateConversation(companyUserId, jobseekerUserId);
        return persistAndDispatch(senderUserId, receiverUserId, conversation, content);
    }

    private Map<String, Object> persistAndDispatch(Long senderUserId, Long receiverUserId,
                                                   Conversation conversation, String content) {
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderUserId(senderUserId);
        message.setReceiverUserId(receiverUserId);
        message.setContent(content);
        message.setReadFlag(0);
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(message);

        Map<String, Object> payload = toMessageView(message);
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(receiverUserId), "/queue/chat", payload);
        notificationService.createAndPush(
                receiverUserId,
                NotificationType.NEW_MESSAGE,
                "\u6536\u5230\u65b0\u7684\u804a\u5929\u6d88\u606f",
                userSupportService.getDisplayName(senderUserId) + " \u5411\u60a8\u53d1\u9001\u4e86\u4e00\u6761\u65b0\u6d88\u606f\u3002",
                senderUserId,
                conversation.getId());
        return payload;
    }

    private Conversation getOrCreateConversation(Long companyUserId, Long jobseekerUserId) {
        Conversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getCompanyUserId, companyUserId)
                .eq(Conversation::getJobseekerUserId, jobseekerUserId));
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setCompanyUserId(companyUserId);
            conversation.setJobseekerUserId(jobseekerUserId);
            conversation.setCreatedAt(LocalDateTime.now());
            conversation.setLastMessageAt(LocalDateTime.now());
            conversationMapper.insert(conversation);
        }
        return conversation;
    }

    private void validateParticipant(Long currentUserId, Conversation conversation) {
        if (conversation == null || (!currentUserId.equals(conversation.getCompanyUserId())
                && !currentUserId.equals(conversation.getJobseekerUserId()))) {
            throw new BusinessException("\u4f1a\u8bdd\u4e0d\u5b58\u5728");
        }
    }

    private void ensureApplicationRelation(Long companyUserId, Long jobseekerUserId) {
        if (!applicationService.existsApplicationBetween(companyUserId, jobseekerUserId)) {
            throw new BusinessException(
                    "\u53cc\u65b9\u9700\u8981\u5b58\u5728\u5c97\u4f4d\u6295\u9012\u5173\u7cfb\u540e\u624d\u53ef\u53d1\u8d77\u804a\u5929");
        }
    }

    private boolean isCompanyAndJobseeker(String currentRole, String peerRole) {
        return (UserRole.COMPANY.name().equals(currentRole) && UserRole.JOBSEEKER.name().equals(peerRole))
                || (UserRole.JOBSEEKER.name().equals(currentRole) && UserRole.COMPANY.name().equals(peerRole));
    }

    private Map<String, Object> toConversationView(Conversation conversation, Long currentUserId) {
        Long peerId = currentUserId.equals(conversation.getCompanyUserId())
                ? conversation.getJobseekerUserId()
                : conversation.getCompanyUserId();
        CompanyProfile company = companyProfileMapper.selectOne(new LambdaQueryWrapper<CompanyProfile>()
                .eq(CompanyProfile::getUserId, conversation.getCompanyUserId()));
        JobseekerProfile jobseeker = jobseekerProfileMapper.selectOne(new LambdaQueryWrapper<JobseekerProfile>()
                .eq(JobseekerProfile::getUserId, conversation.getJobseekerUserId()));
        ChatMessage lastMessage = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getId())
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("limit 1"));
        String companyName = company == null ? userSupportService.getDisplayName(conversation.getCompanyUserId()) : company.getCompanyName();
        String jobseekerName = jobseeker == null ? userSupportService.getDisplayName(conversation.getJobseekerUserId()) : jobseeker.getFullName();
        String peerName = peerId.equals(conversation.getCompanyUserId()) ? companyName : jobseekerName;

        LinkedHashMap<String, Object> view = new LinkedHashMap<>();
        view.put("id", conversation.getId());
        view.put("companyUserId", conversation.getCompanyUserId());
        view.put("jobseekerUserId", conversation.getJobseekerUserId());
        view.put("companyName", companyName);
        view.put("jobseekerName", jobseekerName);
        view.put("peerUserId", peerId);
        view.put("peerName", peerName);
        view.put("lastMessage", lastMessage == null ? "" : lastMessage.getContent());
        view.put("lastMessageAt", conversation.getLastMessageAt());
        return view;
    }

    private Map<String, Object> toMessageView(ChatMessage message) {
        return Map.of(
                "id", message.getId(),
                "conversationId", message.getConversationId(),
                "senderUserId", message.getSenderUserId(),
                "receiverUserId", message.getReceiverUserId(),
                "content", message.getContent(),
                "readFlag", message.getReadFlag(),
                "createdAt", message.getCreatedAt()
        );
    }
}
