package de.sven.bayer.speaking_llm.service;

import de.sven.bayer.speaking_llm.component.TextSplitter;
import de.sven.bayer.speaking_llm.model.conversation.LlmAnswerWithThink;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class TtsService {
    public static final String BASE_URL = "http://localhost:8081";
    private final AudioPlayerService audioPlayerService;
    private final TextSplitter textSplitter;
    private final RestTemplate restTemplate = new RestTemplate();

    public TtsService(AudioPlayerService audioPlayerService, TextSplitter textSplitter) {
        this.audioPlayerService = audioPlayerService;
        this.textSplitter = textSplitter;
    }

    public void playLlmAnswer(LlmAnswerWithThink llmAnswerWithThink) {
        String text = llmAnswerWithThink.answer();
        List<String> sentences = textSplitter.splitIntoSentences(text);
        for (String sentence : sentences) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity<String> entity = new HttpEntity<>(sentence, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    BASE_URL + "/tts",
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );
            byte[] audioAsBytes = response.getBody();
            audioPlayerService.queueAudio(audioAsBytes);
        }
    }
}