package ru.denis.aestymes.dtos;

import lombok.Data;
import java.util.List;

@Data
public class GroupChatDto {
    private String groupName;
    private List<String> memberNames;
}