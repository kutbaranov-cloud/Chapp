//package ru.denis.aestymes.rest_controllers.websocket;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Controller;
//import ru.denis.aestymes.dtos.MessageRequest;
//import ru.denis.aestymes.dtos.MessageUpdateDto;
//import ru.denis.aestymes.services.MessageService;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Controller
//@RequiredArgsConstructor
//public class ChatRestController {
//
//    private final MessageService messageService;
//    private final SimpMessagingTemplate messagingTemplate;
//
//    @MessageMapping("/send/message")
//    public void sendMessage(@Payload MessageRequest request) {
//        var savedMessage = messageService.saveMessageFromRequest(request);
//        var dto = messageService.convertToDto(savedMessage);
//        dto.setType("NEW");
//        messagingTemplate.convertAndSend("/topic/chat/" + request.getChatId(), dto);
//    }
//
//    @MessageMapping("/send/message/edit")
//    public void editMessage(@Payload MessageUpdateDto updateDto) {
//        messageService.editMessage(updateDto.getMessageId(), updateDto.getContent());
//        updateDto.setType("EDIT");
//        messagingTemplate.convertAndSend("/topic/chat/" + updateDto.getChatId(), updateDto);
//    }
//
//    @MessageMapping("/send/message/delete")
//    public void deleteMessage(@Payload MessageUpdateDto updateDto) {
//        messageService.deleteMessage(updateDto.getMessageId());
//        updateDto.setType("DELETE");
//        messagingTemplate.convertAndSend("/topic/chat/" + updateDto.getChatId(), updateDto);
//    }
//
//    @MessageMapping("/send/chat/delete")
//    public void deleteChatNotify(@Payload Map<String, Object> payload) {
//        if (payload != null && payload.containsKey("chatId")) {
//            String chatId = String.valueOf(payload.get("chatId"));
//            Map<String, Object> response = new HashMap<>();
//            response.put("type", "CHAT_DELETED");
//            response.put("chatId", chatId);
//            messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);
//        }
//    }
//}