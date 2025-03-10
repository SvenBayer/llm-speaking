package de.sven.bayer.speaking_llm.model.conversation;

import lombok.Data;

@Data
public class MessageFromUser {
    private String message;
    private String conversationId;
}
