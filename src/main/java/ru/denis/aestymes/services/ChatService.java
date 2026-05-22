package ru.denis.aestymes.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.denis.aestymes.dtos.ChatNameRequest;
import ru.denis.aestymes.dtos.ChatRequest;
import ru.denis.aestymes.models.Chat;
import ru.denis.aestymes.models.ChatMember;
import ru.denis.aestymes.models.MyUser;
import ru.denis.aestymes.repositories.ChatRepository;
import ru.denis.aestymes.repositories.ChatMemberRepository;
import ru.denis.aestymes.repositories.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MyUserService myUserService;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<Chat> getUserChats(Long userId) {
        return chatMemberRepository.findAllByUserId(userId).stream()
                // ХИРУРГИЧЕСКАЯ ОЧИСТКА: Убираем null участников и записи с удаленными чатами
                .filter(java.util.Objects::nonNull)
                .filter(member -> member.getChat() != null && member.getChat().getId() != null)
                .map(member -> {
                    Chat chat = member.getChat();
                    try {
                        messageRepository.findFirstByChatOrderByCreatedAtDesc(chat)
                                .ifPresentOrElse(
                                        chat::setLastMessage,
                                        () -> chat.setLastMessage(null)
                                );
                    } catch (Exception e) {
                        chat.setLastMessage(null);
                    }
                    return chat;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Chat getChatById(Long chatId) {
        return chatRepository.findByIdWithMembers(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
    }

    @Transactional
    public Chat getOrCreatePrivateChat(Long user1Id, Long user2Id) {
        return chatRepository.findPrivateChatBetweenUsers(user1Id, user2Id)
                .orElseGet(() -> {
                    MyUser user1 = myUserService.getUserById(user1Id);
                    MyUser user2 = myUserService.getUserById(user2Id);

                    Chat chat = new Chat();
                    chat.setIsPrivate(true);
                    chat.setIsGroupChat(false);
                    chat.setCreatedBy(user1);
                    chat.setName(user2.getUsername());
                    chat = chatRepository.save(chat);

                    chatMemberRepository.save(ChatMember.builder().chat(chat).user(user1).build());
                    chatMemberRepository.save(ChatMember.builder().chat(chat).user(user2).build());

                    return chat;
                });
    }

    @Transactional
    public void deleteChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        // === ХИРУРГИЧЕСКОЕ ВНЕДРЕНИЕ: Сначала удаляем участников, чтобы не плодить null-призраков ===
        List<ChatMember> members = chatMemberRepository.findAllByChatId(chatId);
        if (members != null && !members.isEmpty()) {
            chatMemberRepository.deleteAll(members);
        }

        chatRepository.delete(chat);
    }

    public void createChat(ChatRequest chatRequest) {
        myUserService.findByUsername(chatRequest.getMember1Username()).ifPresent(u1 -> {
            myUserService.findByUsername(chatRequest.getMember2Username()).ifPresent(u2 -> {
                getOrCreatePrivateChat(u1.getId(), u2.getId());
            });
        });
    }

    public void findChatsByNameWS(ChatNameRequest request) {
    }

    // === ХИРУРГИЧЕСКОЕ ДОБАВЛЕНИЕ: МЕТОД СОЗДАНИЯ ГРУППЫ ===
    @Transactional
    public Chat createGroupChat(String groupName, List<Long> memberIds, Long creatorId) {
        MyUser creator = myUserService.getUserById(creatorId);

        // 1. Создаем сам объект чата
        Chat chat = Chat.builder()
                .name(groupName)
                .isGroupChat(true) // Указываем, что это группа
                .isPrivate(false)
                .createdBy(creator)
                .build();

        Chat savedChat = chatRepository.save(chat);

        // 2. Добавляем всех участников из списка memberIds
        for (Long uid : memberIds) {
            // Просто берем юзера по ID, так как memberIds уже содержит ID всех выбранных людей
            MyUser member = myUserService.getUserById(uid);
            if (member != null) {
                chatMemberRepository.save(ChatMember.builder()
                        .chat(savedChat)
                        .user(member)
                        .unreadCount(0)
                        .build());
            }
        }

        return savedChat;
    }
}