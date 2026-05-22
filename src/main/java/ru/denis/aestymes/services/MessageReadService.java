package ru.denis.aestymes.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.denis.aestymes.repositories.MessageReadRepository;

@Service
public class MessageReadService {

    @Autowired
    private MessageReadRepository messageReadRepository;
}
