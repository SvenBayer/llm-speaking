package de.sven.bayer.speaking_llm.service;

import de.sven.bayer.speaking_llm.model.conversation.LlmAnswerWithThink;
import de.sven.bayer.speaking_llm.model.conversation.MessageFromUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class LlmChatService {
    private static final String LLM_BASE_URL = "http://localhost:8080";
    private final RestTemplate restTemplate = new RestTemplate();

    public LlmAnswerWithThink responseForMessage(MessageFromUser messageFromUser) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MessageFromUser> request = new HttpEntity<>(messageFromUser, headers);

            ResponseEntity<LlmAnswerWithThink> response = restTemplate.exchange(
                    LLM_BASE_URL + "/talktoLLM",
                    HttpMethod.POST,
                    request,
                    LlmAnswerWithThink.class
            );

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error calling LLM service", e);
            return new LlmAnswerWithThink(
                    "Sorry, I couldn't process your message at the moment.",
                    "Error: " + e.getMessage(),
                    messageFromUser.getConversationId()
            );
        }
    }
}