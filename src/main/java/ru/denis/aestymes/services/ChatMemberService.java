package ru.denis.aestymes.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.denis.aestymes.repositories.ChatMemberRepository;

@Service
public class ChatMemberService {

    @Autowired
    private ChatMemberRepository chatMemberRepository;
}
