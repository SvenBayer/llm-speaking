package de.sven.bayer.speaking_llm.controller;

import de.sven.bayer.speaking_llm.model.conversation.LlmAnswerWithThink;
import de.sven.bayer.speaking_llm.model.conversation.MessageFromUser;
import de.sven.bayer.speaking_llm.service.AsrService;
import de.sven.bayer.speaking_llm.service.LlmChatService;
import de.sven.bayer.speaking_llm.service.TtsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final LlmChatService llmChatService;
    private final TtsService ttsService;
    private final AsrService asrService;
    private String conversationId;

    public ChatController(LlmChatService llmChatService, TtsService ttsService, AsrService asrService) {
        this.llmChatService = llmChatService;
        this.ttsService = ttsService;
        this.asrService = asrService;
    }

    @PostMapping("/talktoLLM")
    public void talkToLLM(@RequestBody MessageFromUser messageFromUser) {
        messageFromUser.setConversationId(this.conversationId);
        LlmAnswerWithThink llmAnswerWithThink = llmChatService.responseForMessage(messageFromUser);
        this.conversationId = llmAnswerWithThink.conversationId();
        ttsService.playLlmAnswer(llmAnswerWithThink);
    }

    @GetMapping("/asr")
    public String asr() {
        //return asrService.asrAudio();
        return "hello world";
    }
}
