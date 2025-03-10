// AsrService.java
package de.sven.bayer.speaking_llm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsrService {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String ASR_URL = "http://localhost:9000/asr";
    public static final int SILENCE_THRESHOLD = 500;

    public String transcribeAudio(byte[] audioBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio_file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "recording.wav";
            }
        });
        body.add("model", "large");
        body.add("language", "en");
        body.add("task", "transcribe");
        body.add("encode", "false");

        try {
            return restTemplate.exchange(
                    ASR_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            ).getBody();
        } catch (Exception e) {
            log.error("ASR request failed", e);
            throw new RuntimeException("ASR processing error", e);
        }
    }
}