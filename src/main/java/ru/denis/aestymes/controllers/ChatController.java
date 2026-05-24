package ru.denis.aestymes.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.denis.aestymes.dtos.*;
import ru.denis.aestymes.models.*;
import ru.denis.aestymes.repositories.ChatMemberRepository;
import ru.denis.aestymes.repositories.MessageRepository;
import ru.denis.aestymes.services.ChatService;
import ru.denis.aestymes.services.MessageService;
import ru.denis.aestymes.services.MyUserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MyUserService myUserService;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final ChatMemberRepository chatMemberRepository;

    @MessageMapping("/chat/pin")
    @Transactional
    public void pinMessageNotify(@Payload Map<String, Object> payload, Authentication auth) {
        try {
            if (payload != null && payload.containsKey("chatId") && payload.containsKey("messageId")) {
                Long chatId = Long.valueOf(payload.get("chatId").toString());
                Long messageId = Long.valueOf(payload.get("messageId").toString());
                boolean isPinned = (boolean) payload.get("isPinned");

                log.info("[PIN] Изменение закрепа для сообщения {} в чате {}: {}", messageId, chatId, isPinned);

                Message targetMsg = messageRepository.findById(messageId).orElse(null);
                if (targetMsg != null) {
                    targetMsg.setIsPinned(isPinned);
                    messageRepository.save(targetMsg);
                }

                String actorName = "Пользователь";
                MyUser currentUser = null;
                if (auth != null) {
                    currentUser = myUserService.findByEmail(auth.getName()).orElse(null);
                    if (currentUser != null) {
                        actorName = myUserService.convertToDTO(currentUser).getName();
                    }
                }

                String systemContent;
                if (isPinned) {
                    String cleanText = targetMsg != null ? targetMsg.getContent() : "";
                    String shortContent = cleanText.length() > 25 ? cleanText.substring(0, 25) + "..." : cleanText;
                    systemContent = actorName + " закрепил сообщение" + (shortContent.isEmpty() ? "" : ": \"" + shortContent + "\"");
                } else {
                    systemContent = actorName + " открепил сообщение";
                }

                Message systemMsg = new Message();
                Chat chat = chatService.getChatById(chatId);
                systemMsg.setChat(chat);
                systemMsg.setSender(currentUser);
                systemMsg.setContent(systemContent);
                systemMsg.setCreatedAt(java.time.LocalDateTime.now());

                if (targetMsg != null) {
                    systemMsg.setReplyTo(targetMsg);
                    try {
                        systemMsg.setParentId(messageId);
                    } catch (Exception ignored) {}
                }
                messageRepository.save(systemMsg);

                Map<String, Object> resp = new HashMap<>();
                resp.put("type", isPinned ? "SYSTEM_PIN" : "SYSTEM_UNPIN");
                resp.put("systemMessageId", systemMsg.getId());
                resp.put("id", messageId);
                resp.put("messageId", messageId);
                resp.put("targetMessageId", messageId);
                resp.put("content", systemContent);
                resp.put("chatId", chatId);
                resp.put("isPinned", isPinned);
                resp.put("createdAt", systemMsg.getCreatedAt().toString());

                messagingTemplate.convertAndSend("/topic/chat/" + chatId, resp);

                if (chat != null) {
                    chat.getMembers().forEach(m ->
                            messagingTemplate.convertAndSend("/topic/user/" + m.getUser().getId() + "/queue/messages", resp)
                    );
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при закреплении сообщения: ", e);
        }
    }

    @Transactional(readOnly = true)
    @MessageMapping("/chat/search/global")
    public void searchGlobalMessages(@Payload Map<String, String> payload, Authentication auth) {
        try {
            String query = payload.get("content");
            if (auth == null || query == null || query.isBlank()) return;

            MyUser currentUser = myUserService.findByEmail(auth.getName()).orElse(null);
            if (currentUser == null) return;

            List<ChatMember> memberships = chatMemberRepository.findAllByUserId(currentUser.getId());
            List<Long> chatIds = memberships.stream().map(m -> m.getChat().getId()).collect(Collectors.toList());

            if (chatIds.isEmpty()) return;

            List<Message> foundMessages = messageRepository.findByContentContainingIgnoreCaseAndChatIdIn(query, chatIds);

            List<MessageDto> results = foundMessages.stream()
                    .map(m -> {
                        Chat chat = m.getChat();
                        String chatDisplayName = "Чат";
                        String displayAvatar = null;

                        if (chat.getIsGroupChat() != null && chat.getIsGroupChat()) {
                            chatDisplayName = chat.getName();
                            displayAvatar = chat.getAvatarUrl();
                        } else {
                            List<ChatMember> members = chatMemberRepository.findAllByChatId(chat.getId());
                            MyUser otherUser = members.stream()
                                    .map(ChatMember::getUser)
                                    .filter(u -> !u.getId().equals(currentUser.getId()))
                                    .findFirst()
                                    .orElse(null);
                            if (otherUser != null) {
                                var otherDto = myUserService.convertToDTO(otherUser);
                                chatDisplayName = otherDto.getName();
                                displayAvatar = otherDto.getAvatarUrl();
                            }
                        }

                        var senderDto = myUserService.convertToDTO(m.getSender());

                        return MessageDto.builder()
                                .id(m.getId())
                                .messageId(m.getId())
                                .content(m.getContent())
                                .senderId(m.getSender().getId())
                                .senderNickname(senderDto.getName())
                                .senderAvatar(displayAvatar != null && !displayAvatar.isBlank() ? displayAvatar : null)
                                .chatId(chat.getId())
                                .chatName(chatDisplayName)
                                .createdAt(m.getCreatedAt())
                                .isEdited(m.getIsEdited())
                                .isRead(m.getIsRead())
                                .type("GLOBAL_SEARCH_RESULT")
                                .build();
                    })
                    .collect(Collectors.toList());
            messagingTemplate.convertAndSend("/topic/user/" + currentUser.getId() + "/queue/search-results", results);
        } catch (Exception e) {
            log.error("Error during global message search: ", e);
        }
    }

    @PostMapping("/api/chats/group")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> createGroupChat(@RequestBody GroupChatDto dto, Authentication auth) {
        try {
            MyUser currentUser = myUserService.findByEmail(auth.getName()).orElse(null);
            if (currentUser == null) return ResponseEntity.status(401).build();

            String groupName = dto.getGroupName();
            List<String> memberNames = dto.getMemberNames();

            log.info("[GROUP] Создание: {}. Входящие имена/ID: {}", groupName, memberNames);

            List<Long> memberIds = new ArrayList<>();
            memberIds.add(currentUser.getId());

            if (memberNames != null) {
                for (String name : memberNames) {
                    if (name == null || name.isBlank()) continue;

                    myUserService.findByUsername(name.trim()).ifPresentOrElse(
                            u -> {
                                if (!memberIds.contains(u.getId())) memberIds.add(u.getId());
                            },
                            () -> {
                                try {
                                    Long id = Long.parseLong(name.trim());
                                    myUserService.findById(id).ifPresent(u -> {
                                        if (!memberIds.contains(u.getId())) memberIds.add(u.getId());
                                    });
                                } catch (Exception ignored) {}
                            }
                    );
                }
            }

            if (memberIds.size() > 1) {
                Chat groupChat = chatService.createGroupChat(groupName, memberIds, currentUser.getId());

                Map<String, Object> resp = new HashMap<>();
                resp.put("type", "NEW_CHAT");
                resp.put("chatId", groupChat.getId());
                resp.put("status", "success");

                for (Long id : memberIds) {
                    messagingTemplate.convertAndSend("/topic/user/" + id + "/queue/new-chat", "REFRESH");
                    messagingTemplate.convertAndSend("/topic/user/" + id + "/queue/messages", resp);
                }
                return ResponseEntity.ok(Map.of("status", "success", "chatId", groupChat.getId()));
            } else {
                return ResponseEntity.badRequest().body("Не удалось найти участников.");
            }
        } catch (Exception e) {
            log.error("[GROUP] Ошибка: ", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/api/chats/create")
    @ResponseBody
    public ResponseEntity<?> createChatHttp(@RequestParam String name, Authentication auth) {
        try {
            if (name == null || name.isBlank()) return ResponseEntity.badRequest().body("Invalid data");

            if (auth == null) return ResponseEntity.status(401).build();
            MyUser currentUser = myUserService.findByEmail(auth.getName()).orElse(null);
            if (currentUser == null) return ResponseEntity.status(401).build();

            MyUser target = myUserService.findByUsername(name).orElse(null);
            if (target == null) return ResponseEntity.status(404).body("User not found");

            if (currentUser.getId().equals(target.getId())) {
                return ResponseEntity.badRequest().body("Cannot chat with yourself");
            }

            chatService.getOrCreatePrivateChat(currentUser.getId(), target.getId());

            messagingTemplate.convertAndSend("/topic/user/" + target.getId() + "/queue/new-chat", "REFRESH");
            messagingTemplate.convertAndSend("/topic/user/" + currentUser.getId() + "/queue/new-chat", "REFRESH");

            log.info("HTTP Chat created between {} and {}", currentUser.getUsername(), target.getUsername());
            return ResponseEntity.ok("Created");
        } catch (Exception e) {
            log.error("Error in HTTP chat creation", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @MessageMapping("/chat/{chatId}/typing")
    public void processTyping(@DestinationVariable Long chatId, @Payload TypingDto typingDto) {
        typingDto.setChatId(chatId);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/typing", typingDto);
    }

    @Transactional(readOnly = true)
    @MessageMapping("/chat/{chatId}/search/messages")
    public void searchMessages(@DestinationVariable Long chatId, @Payload Map<String, String> payload, Authentication auth) {
        try {
            String query = payload.get("content");
            if (auth == null || query == null || query.isBlank()) return;
            log.info("Searching for '{}' in chat {}", query, chatId);

            List<Message> foundMessages = messageRepository.findByChatIdAndContentContainingIgnoreCase(chatId, query);

            List<MessageDto> results = foundMessages.stream()
                    .map(m -> {
                        var senderDto = myUserService.convertToDTO(m.getSender());

                        return MessageDto.builder()
                                .id(m.getId())
                                .messageId(m.getId())
                                .content(m.getContent())
                                .senderId(m.getSender().getId())
                                .senderNickname(senderDto.getName())
                                .senderAvatar(senderDto.getAvatarUrl())
                                .chatId(chatId)
                                .createdAt(m.getCreatedAt())
                                .isEdited(m.getIsEdited())
                                .isRead(m.getIsRead())
                                .type("SEARCH_RESULT")
                                .build();
                    })
                    .collect(Collectors.toList());
            messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/search/results", results);
        } catch (Exception e) {
            log.error("Error during message search: ", e);
        }
    }

    @MessageMapping("/send/create/chat")
    public void createChatSocket(@Payload Map<String, String> payload, Authentication auth) {
        try {
            String targetUsername = payload.get("name");
            if (auth == null || targetUsername == null) return;
            MyUser currentUser = myUserService.findByEmail(auth.getName()).orElse(null);
            if (currentUser == null) return;
            myUserService.findByUsername(targetUsername).ifPresentOrElse(target -> {
                chatService.getOrCreatePrivateChat(currentUser.getId(), target.getId());
                messagingTemplate.convertAndSend("/topic/user/" + currentUser.getId() + "/queue/new-chat", "REFRESH");
                messagingTemplate.convertAndSend("/topic/user/" + target.getId() + "/queue/new-chat", "REFRESH");
            }, () -> log.warn("Target user not found: {}", targetUsername));
        } catch (Exception e) { log.error("Error during chat creation: ", e); }
    }

    @MessageMapping("/chat/{chatId}/read")
    public void markMessagesAsRead(@DestinationVariable Long chatId, @Payload Map<String, Long> payload) {
        Long readerId = payload.get("senderId");
        Chat chat = chatService.getChatById(chatId);
        if (chat != null && readerId != null) {
            messageRepository.markMessagesAsRead(chat, readerId);
            chatMemberRepository.findByChatIdAndUserId(chatId, readerId).ifPresent(member -> {
                member.setUnreadCount(0);
                chatMemberRepository.save(member);
            });
            Map<String, Object> response = new HashMap<>();
            response.put("type", "READ_ALL");
            response.put("chatId", chatId);
            response.put("readerId", readerId);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);
            chat.getMembers().forEach(m -> messagingTemplate.convertAndSend("/topic/user/" + m.getUser().getId() + "/queue/messages", response));
        }
    }

    @ResponseBody
    @PostMapping("/api/chats/{chatId}/read")
    public void markMessagesAsReadHttp(@PathVariable Long chatId, HttpServletRequest request) {
        MyUser currentUser = getAuthenticatedUser(request);
        if (currentUser != null) {
            Chat chat = chatService.getChatById(chatId);
            if (chat != null) {
                messageRepository.markMessagesAsRead(chat, currentUser.getId());
                chatMemberRepository.findByChatIdAndUserId(chatId, currentUser.getId()).ifPresent(member -> {
                    member.setUnreadCount(0);
                    chatMemberRepository.save(member);
                });
                Map<String, Object> resp = new HashMap<>();
                resp.put("type", "READ_ALL"); resp.put("chatId", chatId); resp.put("readerId", currentUser.getId());
                messagingTemplate.convertAndSend("/topic/chat/" + chatId, resp);
                chat.getMembers().forEach(m -> messagingTemplate.convertAndSend("/topic/user/" + m.getUser().getId() + "/queue/messages", resp));
            }
        }
    }

    @GetMapping("/chats")
    public String getChats(Model model, HttpServletRequest request) {
        MyUser currentUser = getAuthenticatedUser(request);
        if (currentUser == null) return "redirect:/login";
        prepareChatModel(model, currentUser);
        return "chat/allChats";
    }

    @GetMapping("/chats/{chatId}")
    public String getChat(@PathVariable Long chatId, Model model, HttpServletRequest request) {
        try {
            MyUser currentUser = getAuthenticatedUser(request);
            if (currentUser == null) return "redirect:/login";
            Chat chat = chatService.getChatById(chatId);
            if (chat == null) return "redirect:/chats";

            boolean isMember = chat.getMembers().stream()
                    .anyMatch(member -> member.getUser().getId().equals(currentUser.getId()));
            if (!isMember) {
                log.warn("Попытка доступа к чату {}, в котором юзер {} не состоит", chatId, currentUser.getUsername());
                return "redirect:/chats";
            }

            prepareChatModel(model, currentUser);

            if (chat.getIsGroupChat() != null && chat.getIsGroupChat()) {
                Map<String, Object> groupHeader = new HashMap<>();
                groupHeader.put("name", chat.getName());
                groupHeader.put("avatarUrl", chat.getAvatarUrl());
                model.addAttribute("headerUser", groupHeader);
            } else {
                MyUser other = chat.getMembers().get(0).getUser().getId().equals(currentUser.getId())
                        ? chat.getMembers().get(1).getUser() : chat.getMembers().get(0).getUser();
                model.addAttribute("headerUser", myUserService.convertToDTO(other));
            }

            model.addAttribute("chat", chat);

            List<Message> messages = messageService.getMessagesByChat(chat);
            model.addAttribute("messages", messages);

            Message pinnedMessage = messages.stream()
                    .filter(m -> m.getIsPinned() != null && m.getIsPinned())
                    .reduce((first, second) -> second)
                    .orElse(null);
            model.addAttribute("pinnedMessage", pinnedMessage);

            Long firstUnreadId = messages.stream()
                    .filter(m -> m.getSender() != null && !m.getSender().getId().equals(currentUser.getId()) && (m.getIsRead() == null || !m.getIsRead()))
                    .map(Message::getId)
                    .findFirst()
                    .orElse(null);
            model.addAttribute("firstUnreadMessageId", firstUnreadId);

            messageRepository.markMessagesAsRead(chat, currentUser.getId());
            chatMemberRepository.findByChatIdAndUserId(chatId, currentUser.getId()).ifPresent(m -> {
                m.setUnreadCount(0);
                chatMemberRepository.save(m);
            });

            return "chat/allChats";
        } catch (Exception e) {
            log.error("Chat loading error: ", e);
            return "redirect:/chats";
        }
    }

    @GetMapping("/api/chats/{chatId}/pinned")
    @ResponseBody
    public List<Long> getPinnedMessageIds(@PathVariable Long chatId) {
        Chat chat = chatService.getChatById(chatId);
        if (chat == null) return new ArrayList<>();

        return messageRepository.findByChatOrderByCreatedAtAsc(chat).stream()
                .filter(m -> m.getIsPinned() != null && m.getIsPinned())
                .map(Message::getId)
                .collect(Collectors.toList());
    }

    @GetMapping("/api/chats/{chatId}/pinned-details")
    @ResponseBody
    public List<Map<String, Object>> getPinnedMessageDetails(@PathVariable Long chatId) {
        Chat chat = chatService.getChatById(chatId);
        if (chat == null) return new ArrayList<>();

        return messageRepository.findByChatOrderByCreatedAtAsc(chat).stream()
                .filter(m -> m.getIsPinned() != null && m.getIsPinned())
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("content", m.getContent());
                    map.put("senderName", m.getSender() != null ? m.getSender().getName() : "Система");
                    return map;
                })
                .collect(Collectors.toList());
    }

    private void prepareChatModel(Model model, MyUser currentUser) {
        model.addAttribute("chats", chatService.getUserChats(currentUser.getId()));
        model.addAttribute("currentUser", myUserService.convertToDTO(currentUser));
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("nickname", currentUser.getUsername());
    }

    private MyUser getAuthenticatedUser(HttpServletRequest request) {
        Long id = myUserService.getCurrentUserId(request);
        if (id != null && id != -1L) return myUserService.getUserById(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return myUserService.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    @PostMapping("/chats/{id}/delete")
    public String deleteChat(@PathVariable Long id) {
        try { if (id != null) chatService.deleteChat(id); } catch (Exception e) { log.warn("Chat already gone: {}", id); }
        return "redirect:/chats";
    }

    @Transactional
    @MessageMapping("/send/message")
    public void sendMessage(@Payload MessageDto dto) {
        try {
            Message saved = messageService.saveMessage(dto.getChatId(), dto.getSenderId(), dto.getContent(), dto.getParentId());

            if (dto.getParentContent() != null && dto.getParentContent().startsWith("FORWARDED:")) {
                saved.setParentContent(dto.getParentContent());
                saved.setParentSenderName(dto.getParentSenderName());
                saved.setParentSenderAvatar(dto.getParentSenderAvatar());
                messageRepository.save(saved);
            }

            ru.denis.aestymes.dtos.UserDTO senderDto = myUserService.convertToDTO(saved.getSender());

            MessageDto out = MessageDto.builder()
                    .id(saved.getId())
                    .messageId(saved.getId())
                    .content(saved.getContent())
                    .senderId(saved.getSender().getId())
                    .senderNickname(senderDto.getName())
                    .senderAvatar(senderDto.getAvatarUrl())
                    .chatId(dto.getChatId())
                    .createdAt(saved.getCreatedAt())
                    .isEdited(false)
                    .isRead(false)
                    .type("NEW")
                    .parentId(saved.getReplyTo() != null ? saved.getReplyTo().getId() : null)
                    .parentSenderAvatar(saved.getReplyTo() != null ?
                            myUserService.convertToDTO(saved.getReplyTo().getSender()).getAvatarUrl() :
                            saved.getParentSenderAvatar())
                    .parentContent(saved.getReplyTo() != null ? saved.getReplyTo().getContent() : saved.getParentContent())
                    .parentSenderName(saved.getReplyTo() != null ? saved.getReplyTo().getSender().getName() : saved.getParentSenderName())
                    .build();

            messagingTemplate.convertAndSend("/topic/chat/" + out.getChatId(), out);

            Chat chat = chatService.getChatById(out.getChatId());
            if (chat != null) {
                chat.getMembers().forEach(m ->
                        messagingTemplate.convertAndSend("/topic/user/" + m.getUser().getId() + "/queue/messages", out)
                );
            }
        } catch (Exception e) {
            log.error("КРИТИЧЕСКАЯ ОШИБКА при отправке сообщения через WebSocket: ", e);
        }
    }

    @MessageMapping("/send/message/edit")
    public void editMessage(@Payload MessageDto dto) {
        messageService.editMessage(dto.getMessageId(), dto.getContent());
        dto.setType("EDIT");
        messagingTemplate.convertAndSend("/topic/chat/" + dto.getChatId(), dto);
        Chat chat = chatService.getChatById(dto.getChatId());
        if (chat != null) chat.getMembers().forEach(m -> messagingTemplate.convertAndSend("/topic/user/" + m.getUser().getId() + "/queue/messages", dto));
    }

    @MessageMapping("/send/message/delete")
    public void deleteMessage(@Payload MessageDto dto) {
        Message msg = messageRepository.findById(dto.getMessageId()).orElse(null);
        boolean wasUnread = (msg != null && !msg.getIsRead());
        messageService.deleteMessage(dto.getMessageId());
        Chat chat = chatService.getChatById(dto.getChatId());

        Map<String, Object> resp = new HashMap<>();
        resp.put("type", "DELETE");
        resp.put("messageId", dto.getMessageId());
        resp.put("chatId", dto.getChatId());
        resp.put("wasUnread", wasUnread);

        messageRepository.findFirstByChatOrderByCreatedAtDesc(chat).ifPresentOrElse(last -> {
            resp.put("newLastContent", last.getContent());
            resp.put("newLastTime", last.getCreatedAt().toString());
            resp.put("lastSenderId", last.getSender() != null ? last.getSender().getId() : null);
            resp.put("lastIsRead", last.getIsRead() != null ? last.getIsRead() : true);
        }, () -> resp.put("newLastContent", null));

        if (chat != null) chat.getMembers().forEach(m -> messagingTemplate.convertAndSend("/topic/user/" + m.getUser().getId() + "/queue/messages", resp));
        messagingTemplate.convertAndSend("/topic/chat/" + dto.getChatId(), resp);
    }

    @PostMapping("/api/chats/{chatId}/leave")
    @ResponseBody
    public ResponseEntity<?> leaveGroupChat(@PathVariable Long chatId, Authentication auth) {
        try {
            MyUser currentUser = myUserService.findByEmail(auth.getName()).orElse(null);
            if (currentUser == null) return ResponseEntity.status(401).build();

            String displayedName = "Пользователь";
            ru.denis.aestymes.dtos.UserDTO userDto = myUserService.convertToDTO(currentUser);
            if (userDto != null && userDto.getName() != null && !userDto.getName().isBlank()) {
                displayedName = userDto.getName().trim().split("\\s+")[0];
            } else if (currentUser.getUsername() != null && !currentUser.getUsername().isBlank()) {
                displayedName = currentUser.getUsername().trim().split("\\s+")[0];
            }

            Chat currentChat = chatService.getChatById(chatId);
            if (currentChat != null) {
                Message systemMsg = new Message();
                systemMsg.setChat(currentChat);
                systemMsg.setSender(null);
                systemMsg.setContent(displayedName + " покинул(а) группу");
                systemMsg.setCreatedAt(java.time.LocalDateTime.now());
                messageRepository.save(systemMsg);
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("type", "USER_LEFT");
            resp.put("chatId", chatId);
            resp.put("userId", currentUser.getId());
            resp.put("userName", displayedName);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, resp);

            chatService.leaveGroupChat(chatId, currentUser.getId());

            return ResponseEntity.ok(Map.of("status", "success", "message", "Вы покинули группу"));
        } catch (Exception e) {
            log.error("Ошибка при выходе из группы: ", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    public static class AddMembersDto {
        private List<Long> userIds;
        public List<Long> getUserIds() { return userIds; }
        public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
    }

    @PostMapping("/api/chats/{chatId}/add-members")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> addMembersToGroup(@PathVariable Long chatId, @RequestBody AddMembersDto payload, Authentication auth) {
        try {
            MyUser currentUser = myUserService.findByEmail(auth.getName()).orElse(null);
            if (currentUser == null) return ResponseEntity.status(401).build();

            if (payload.getUserIds() == null || payload.getUserIds().isEmpty()) {
                return ResponseEntity.badRequest().body("Не выбраны пользователи для добавления");
            }

            List<MyUser> addedUsers = chatService.addMembersToGroup(chatId, payload.getUserIds());

            if (addedUsers.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "Пользователи уже состоят в группе"));
            }

            String actorName = currentUser.getName() != null ? currentUser.getName().trim().split("\\s+")[0] : currentUser.getUsername();

            Chat chat = chatService.getChatById(chatId);

            for (MyUser addedUser : addedUsers) {
                String addedName = addedUser.getName() != null ? addedUser.getName().trim().split("\\s+")[0] : addedUser.getUsername();
                String noticeText = actorName + " добавил(а) " + addedName;

                Message systemMsg = new Message();
                systemMsg.setChat(chat);
                systemMsg.setSender(null);
                systemMsg.setContent(noticeText);
                systemMsg.setCreatedAt(java.time.LocalDateTime.now());
                messageRepository.save(systemMsg);

                Map<String, Object> resp = new HashMap<>();
                resp.put("type", "USER_ADDED");
                resp.put("chatId", chatId);
                resp.put("content", noticeText);

                messagingTemplate.convertAndSend("/topic/chat/" + chatId, resp);
                messagingTemplate.convertAndSend("/topic/user/" + addedUser.getId() + "/queue/new-chat", "REFRESH");
            }

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Ошибка при добавлении в группу: ", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // ИЗМЕНЕНИЯ: Удален MultipartFile, принимаем только строку avatarUrl
    @PostMapping("/api/chats/{chatId}/update")
    @ResponseBody
    public ResponseEntity<?> updateGroupSettings(@PathVariable Long chatId,
                                                 @RequestParam("name") String name,
                                                 @RequestParam(value = "avatarUrl", required = false) String avatarUrl,
                                                 Authentication auth) {
        try {
            MyUser currentUser = myUserService.findByEmail(auth.getName()).orElse(null);
            if (currentUser == null) return ResponseEntity.status(401).build();

            chatService.updateGroupSettings(chatId, name, avatarUrl);

            String actorName = currentUser.getName() != null ? currentUser.getName().trim().split("\\s+")[0] : currentUser.getUsername();
            String noticeText = actorName + " изменил(а) настройки группы";

            Chat chat = chatService.getChatById(chatId);
            Message systemMsg = new Message();
            systemMsg.setChat(chat);
            systemMsg.setSender(null);
            systemMsg.setContent(noticeText);
            systemMsg.setCreatedAt(java.time.LocalDateTime.now());
            messageRepository.save(systemMsg);

            Map<String, Object> resp = new HashMap<>();
            resp.put("type", "SYSTEM_NOTICE");
            resp.put("chatId", chatId);
            resp.put("content", noticeText);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, resp);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка при обновлении группы: ", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @MessageMapping("/send/chat/delete")
    public void deleteChatNotify(@Payload Map<String, Object> payload) {
        if (payload != null && payload.containsKey("chatId")) {
            String cid = String.valueOf(payload.get("chatId"));
            Map<String, Object> resp = new HashMap<>();
            resp.put("type", "CHAT_DELETED"); resp.put("chatId", cid);
            messagingTemplate.convertAndSend("/topic/chat/" + cid, resp);
        }
    }
}