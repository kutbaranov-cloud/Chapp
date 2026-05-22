package ru.denis.aestymes.services;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.denis.aestymes.dtos.*;
import ru.denis.aestymes.models.*;
import ru.denis.aestymes.repositories.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ChatService chatService;
    @Autowired
    private MyUserRepository myUserRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private ChatMemberRepository chatMemberRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<Message> getMessagesByChat(Chat chat) {
        return messageRepository.findByChatOrderByCreatedAtAsc(chat);
    }

    public List<MessageDto> convertToDtoList(List<Message> messages) {
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Message getMessageById(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
    }

    @Transactional
    public Message saveMessageFromRequest(MessageRequest request) {
        Chat chat = chatService.getChatById(request.getChatId());
        MyUser sender = myUserRepository.getMyUserById(request.getSenderId());

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setIsEdited(false);
        message.setIsRead(false);

        Message savedMessage = messageRepository.save(message);

        chat.setLastMessage(savedMessage);
        chatRepository.save(chat);

        chat.getMembers().stream()
                .filter(m -> !m.getUser().getId().equals(sender.getId()))
                .forEach(recipient -> {
                    Integer current = recipient.getUnreadCount();
                    recipient.setUnreadCount((current == null ? 0 : current) + 1);
                    chatMemberRepository.save(recipient);
                });

        return savedMessage;
    }

    @Transactional
    public void markChatAsRead(Long chatId, Long userId) {
        Chat chat = chatService.getChatById(chatId);
        messageRepository.markMessagesAsRead(chat, userId);

        chatMemberRepository.findByChatIdAndUserId(chatId, userId).ifPresent(member -> {
            member.setUnreadCount(0);
            chatMemberRepository.save(member);
        });

        messagingTemplate.convertAndSend("/topic/chat/" + chatId,
                Map.of("type", "READ_ALL", "chatId", chatId));
    }

    public MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        if (message.getSender() != null) {
            dto.setSenderNickname(message.getSender().getName());
            dto.setSenderId(message.getSender().getId());
        }
        if (message.getChat() != null) dto.setChatId(message.getChat().getId());
        dto.setIsEdited(message.getIsEdited() != null && message.getIsEdited());
        dto.setIsRead(message.getIsRead() != null && message.getIsRead());

        dto.setParentId(message.getParentId());
        dto.setParentContent(message.getParentContent());
        dto.setParentSenderName(message.getParentSenderName());

        return dto;
    }

    @Transactional
    public void editMessage(Long messageId, String newContent) {
        Message message = getMessageById(messageId);
        message.setContent(newContent);
        message.setIsEdited(true);
        messageRepository.save(message);

        // --- ХИРУРГИЧЕСКОЕ ОБНОВЛЕНИЕ ЦИТАТ В БАЗЕ ---
        // Чтобы после перезагрузки страницы ответы тоже показывали новый текст
        messageRepository.findAllByReplyTo(message).forEach(reply -> {
            reply.setParentContent(newContent);
            messageRepository.save(reply);
        });

        // --- СИГНАЛ ДЛЯ ФРОНТЕНДА ---
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(),
                Map.of(
                        "type", "EDIT",
                        "messageId", messageId,
                        "newContent", newContent,
                        "isUpdateReplies", true // Флаг, чтобы JS обновил цитаты
                ));
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        Message message = getMessageById(messageId);
        Chat chat = message.getChat();
        Long chatId = chat.getId();

        // 1. Обновляем ответы в базе (для тех, кто обновит страницу)
        messageRepository.findAllByReplyTo(message).forEach(reply -> {
            reply.setReplyTo(null);
            reply.setParentId(null);
            reply.setParentContent("Сообщение удалено");
            messageRepository.save(reply);
        });

        if (chat.getLastMessage() != null && chat.getLastMessage().getId().equals(messageId)) {
            chat.setLastMessage(null);
            chatRepository.save(chat);
        }

        messageRepository.delete(message);

        // --- ХИРУРГИЧЕСКАЯ МОДИФИКАЦИЯ ОТВЕТА ---
        messagingTemplate.convertAndSend("/topic/chat/" + chatId,
                Map.of(
                        "type", "DELETE",
                        "messageId", messageId,
                        "chatId", chatId,
                        "updatedParentContent", "Сообщение удалено"
                ));
    }

    @Transactional
    public Message saveMessage(Long chatId, Long senderId, String content, Long parentId) {
        Chat chat = chatService.getChatById(chatId);
        MyUser sender = myUserRepository.getMyUserById(senderId);

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        message.setIsEdited(false);
        message.setIsRead(false);

        if (parentId != null) {
            messageRepository.findById(parentId).ifPresent(parentMsg -> {
                message.setReplyTo(parentMsg);
                message.setParentId(parentMsg.getId());
                message.setParentContent(parentMsg.getContent());
                if (parentMsg.getSender() != null) {
                    message.setParentSenderName(parentMsg.getSender().getName());
                }
            });
        }

        Message savedMessage = messageRepository.save(message);

        chat.setLastMessage(savedMessage);
        chatRepository.save(chat);

        chat.getMembers().stream()
                .filter(m -> !m.getUser().getId().equals(senderId))
                .forEach(recipient -> {
                    Integer current = recipient.getUnreadCount();
                    recipient.setUnreadCount((current == null ? 0 : current) + 1);
                    chatMemberRepository.save(recipient);
                });

        return savedMessage;
    }

    @Transactional
    public Message saveMessage(Long chatId, Long senderId, String content) {
        return saveMessage(chatId, senderId, content, null);
    }
}