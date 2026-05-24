package ru.denis.aestymes.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MyUserService myUserService;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<Chat> getUserChats(Long userId) {
        return chatMemberRepository.findAllByUserId(userId).stream()
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
                // ХИРУРГИЧЕСКОЕ ВНЕДРЕНИЕ: Сортировка чатов по времени последнего сообщения
                .sorted((c1, c2) -> {
                    java.time.LocalDateTime t1 = c1.getLastMessage() != null ? c1.getLastMessage().getCreatedAt() : null;
                    java.time.LocalDateTime t2 = c2.getLastMessage() != null ? c2.getLastMessage().getCreatedAt() : null;

                    if (t1 != null && t2 != null) {
                        return t2.compareTo(t1); // У обоих есть сообщения, сортируем по времени
                    } else if (t1 != null) {
                        return -1; // У с1 есть сообщения, он выше
                    } else if (t2 != null) {
                        return 1; // У с2 есть сообщения, он выше
                    } else {
                        return Long.compare(c2.getId(), c1.getId()); // Пустые чаты сортируем по ID (новые выше)
                    }
                })
                // КОНЕЦ ВНЕДРЕНИЯ
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
                    return chatRepository.findPrivateChatBetweenUsers(user2Id, user1Id)
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
                });
    }

    @Transactional
    public List<MyUser> addMembersToGroup(Long chatId, List<Long> userIdsToAdd) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (chat.getIsGroupChat() == null || !chat.getIsGroupChat()) {
            throw new RuntimeException("Этот чат не является группой");
        }

        List<Long> currentMemberIds = chatMemberRepository.findAllByChatId(chatId).stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toList());

        List<MyUser> newlyAddedUsers = new java.util.ArrayList<>();

        for (Long uid : userIdsToAdd) {
            if (!currentMemberIds.contains(uid)) {
                MyUser member = myUserService.getUserById(uid);
                if (member != null) {
                    chatMemberRepository.save(ChatMember.builder()
                            .chat(chat)
                            .user(member)
                            .unreadCount(0)
                            .build());
                    newlyAddedUsers.add(member);
                }
            }
        }
        return newlyAddedUsers;
    }

    @Transactional
    public void deleteChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

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

    @Transactional
    public void leaveGroupChat(Long chatId, Long userId) {
        chatMemberRepository.findByChatIdAndUserId(chatId, userId).ifPresentOrElse(
                member -> {
                    Chat chat = member.getChat();
                    if (chat != null && chat.getMembers() != null) {
                        chat.getMembers().remove(member);
                    }
                    chatMemberRepository.delete(member);
                    chatMemberRepository.flush();
                    log.info("Пользователь {} покинул группу {}", userId, chatId);

                    if (chat != null && (chat.getMembers() == null || chat.getMembers().isEmpty())) {
                        log.info("Группа {} пуста, удаляем её полностью", chatId);
                        deleteChat(chatId);
                    }
                },
                () -> {
                    throw new RuntimeException("Вы не являетесь участником этой группы");
                }
        );
    }

    @Transactional
    public void updateGroupSettings(Long chatId, String newName, org.springframework.web.multipart.MultipartFile avatarFile) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        chat.setName(newName);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String uploadDir = "uploads/";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String fileName = "avatar_chat_" + chatId + "_" + System.currentTimeMillis() + "_" + avatarFile.getOriginalFilename();
                java.nio.file.Path filePath = java.nio.file.Paths.get(uploadDir + fileName);

                java.nio.file.Files.copy(avatarFile.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                chat.setAvatarUrl("/uploads/" + fileName);
            } catch (Exception e) {
                log.error("Ошибка сохранения файла: ", e);
                throw new RuntimeException("Ошибка сохранения файла");
            }
        }

        chatRepository.save(chat);
    }


    @Transactional
    public Chat createGroupChat(String groupName, List<Long> memberIds, Long creatorId) {
        MyUser creator = myUserService.getUserById(creatorId);

        Chat chat = Chat.builder()
                .name(groupName)
                .isGroupChat(true)
                .isPrivate(false)
                .createdBy(creator)
                .build();

        Chat savedChat = chatRepository.save(chat);

        for (Long uid : memberIds) {
            MyUser member = myUserService.getUserById(uid);
            if (member != null) {
                chatMemberRepository.save(ChatMember.builder()
                        .chat(savedChat)
                        .user(member)
                        .unreadCount(0)
                        .build());
            }
        }


        // --- ДОБАВЛЕНО: Системное сообщение о создании группы ---
        ru.denis.aestymes.models.Message systemMsg = new ru.denis.aestymes.models.Message();
        systemMsg.setChat(savedChat);
        systemMsg.setSender(null); // null означает, что это системное (серое) сообщение
        systemMsg.setContent(creator.getName() + " создал(а) группу");
        systemMsg.setCreatedAt(java.time.LocalDateTime.now());
        messageRepository.save(systemMsg);
        // --------------------------------------------------------

        return savedChat;
    }
}