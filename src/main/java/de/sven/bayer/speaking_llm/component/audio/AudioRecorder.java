package de.sven.bayer.speaking_llm.component.audio;

import de.sven.bayer.speaking_llm.model.conversation.LlmAnswerWithThink;
import de.sven.bayer.speaking_llm.model.conversation.MessageFromUser;
import de.sven.bayer.speaking_llm.service.AsrService;
import de.sven.bayer.speaking_llm.service.AudioPlayerService;
import de.sven.bayer.speaking_llm.service.LlmChatService;
import de.sven.bayer.speaking_llm.service.TtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class AudioRecorder {

    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            16000,     // 16kHz sample rate (common for ASR)
            16,        // 16-bit samples
            1,         // Mono channel
            true,      // Signed PCM
            false      // Little-endian byte order
    );
    public static final int MIN_AUDIO_LENGTH_MS = 200;
    private static final int PROCESSING_THREADS = 4;
    public static final int MIN_SILENCE_DURATION = 3000;

    private final AsrService asrService;
    private final LlmChatService llmChatService;
    private final TtsService ttsService;
    private final AudioPlayerService audioPlayerService;

    private ExecutorService processingPool;
    private volatile boolean running;
    private TargetDataLine dataLine;
    private Thread recordingThread;

    private StringBuilder builder;

    public AudioRecorder(AsrService asrService, LlmChatService llmChatService, TtsService ttsService, AudioPlayerService audioPlayerService) {
        this.asrService = asrService;
        this.llmChatService = llmChatService;
        this.ttsService = ttsService;
        this.audioPlayerService = audioPlayerService;
    }

    public synchronized void start() {
        if (running) return;

        running = true;
        recordingThread = new Thread(this::captureAudio);
        recordingThread.start();
    }

    private void captureAudio() {
        while (running) {
            boolean audioFinished = false;
            while (!audioFinished) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                audioFinished = audioPlayerService.isAudioFinished();
            }
            try {
                builder = new StringBuilder();
                processingPool = Executors.newFixedThreadPool(PROCESSING_THREADS);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
                dataLine = (TargetDataLine) AudioSystem.getLine(info);
                dataLine.open(AUDIO_FORMAT);
                dataLine.start();

                processAudioStream();
            } catch (LineUnavailableException e) {
                log.error("Microphone unavailable: {}", e.getMessage());
            } finally {
                cleanup();
                System.out.println("FINAL Transcribed audio: " + builder.toString());
            }
            sendRequestToLlm(builder.toString());
        }
    }

    private String conversationId;

    public void sendRequestToLlm(String messageContent) {
        MessageFromUser messageFromUser = new MessageFromUser();
        messageFromUser.setMessage(messageContent);
        messageFromUser.setConversationId(this.conversationId);
        LlmAnswerWithThink llmAnswerWithThink = llmChatService.responseForMessage(messageFromUser);
        this.conversationId = llmAnswerWithThink.conversationId();
        ttsService.playLlmAnswer(llmAnswerWithThink);
    }

    private void processAudioStream() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        long lastSoundTime = 0;
        boolean soundDetected = false;
        boolean initialSilence = true;

        System.out.println("Talk to Lumi!");

        while (running) {
            int bytesRead = readChunk(chunk);
            if (bytesRead <= 0) continue;

            buffer.write(chunk, 0, bytesRead);

            boolean currentSound = containsSound(chunk, bytesRead);
            long currentTime = System.currentTimeMillis();

            if (currentSound) {
                if (initialSilence) {
                    // First sound detected, start recording proper
                    initialSilence = false;
                    buffer.reset(); // Clear any pre-sound silence
                }
                lastSoundTime = currentTime;
                soundDetected = true;
            }

            if (!initialSilence) {
                // Check for processing conditions
                boolean silenceDurationMet = (currentTime - lastSoundTime) >= MIN_SILENCE_DURATION;
                boolean minimumAudioMet = buffer.size() >= getMinimumAudioBytes();

                if (silenceDurationMet || minimumAudioMet) {
                    // Process audio if we have either 3s silence or enough audio
                    if (soundDetected) {
                        byte[] audioData = buffer.toByteArray();
                        processingPool.submit(() -> processAudioSegment(audioData));
                        buffer.reset();
                        soundDetected = false;
                    }

                    if (silenceDurationMet) {
                        // Stop recording after 3s silence
                        System.out.println("Wait for Lumi's response");
                        return;
                    }
                }
            }
        }
    }

    // Improved sound detection (check every 8 samples)
    private boolean containsSound(byte[] buffer, int length) {
        int soundDetected = 0;
        for (int i = 0; i < length; i += 8) { // 8 samples (16 bytes)
            if (i+1 >= length) break;
            short sample = (short) ((buffer[i] & 0xFF) | (buffer[i+1] << 8));
            if (Math.abs(sample) > 10000) { // Lower threshold for better sensitivity
                soundDetected++;
                if (soundDetected > 4) return true;
            }
        }
        return false;
    }

    private int readChunk(byte[] buffer) {
        try {
            return dataLine.read(buffer, 0, buffer.length);
        } catch (Exception e) {
            log.error("Audio read error: {}", e.getMessage());
            return -1;
        }
    }

    // Change the pause threshold to 90ms
    private boolean shouldProcess(ByteArrayOutputStream buffer, long lastSoundTime) {
        return buffer.size() >= getMinimumAudioBytes() &&
                (System.currentTimeMillis() - lastSoundTime) >= MIN_AUDIO_LENGTH_MS;
    }

    // This returns the minimum number of audio bytes required.
    private int getMinimumAudioBytes() {
        // 1 second of audio: 16000 samples * 2 bytes/sample = 32000 bytes
        return 64000;
    }

    private void processAudioSegment(byte[] pcmData) {
        try {
            byte[] wavData = convertToWav(pcmData);
            String transcription = asrService.transcribeAudio(wavData);

            synchronized(builder) {
                if (transcription != null) {
                    if (transcription.endsWith("\n")) {
                        transcription = transcription.substring(0, transcription.length() - 1);
                    }
                    builder.append(transcription).append(" ");
                    System.out.println(builder);
                }
            }
        } catch (IOException e) {
            log.error("Processing failed", e);
        }
    }

    private byte[] convertToWav(byte[] pcmData) throws IOException {
        //byte[] normalizedPcm = normalizeAudio(pcmData); // Apply normalization
        byte[] normalizedPcm = pcmData;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(normalizedPcm);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            AudioInputStream ais = new AudioInputStream(
                    bais,
                    AUDIO_FORMAT,
                    normalizedPcm.length / AUDIO_FORMAT.getFrameSize()
            );

            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);
            return baos.toByteArray();
        }
    }

    private byte[] normalizeAudio(byte[] pcmData) {
        // Convert bytes to 16-bit samples
        short[] samples = new short[pcmData.length / 2];
        for (int i = 0; i < samples.length; i++) {
            int low = pcmData[2*i] & 0xFF;
            int high = pcmData[2*i + 1];
            samples[i] = (short) ((high << 8) | low);
        }

        // Find peak amplitude
        short maxPeak = 0;
        for (short sample : samples) {
            short abs = (short) Math.abs(sample);
            if (abs > maxPeak) maxPeak = abs;
        }

        // Calculate gain factor (boost to 90% of max volume)
        double gain;
        if (maxPeak == 0) {
            gain = 1.0; // Silence, no adjustment
        } else {
            double targetPeak = 0.9 * Short.MAX_VALUE; // 90% of maximum volume
            gain = targetPeak / maxPeak;
            gain = Math.min(gain, 4.0); // Limit maximum boost to 4x
        }

        // Apply gain and convert back to bytes
        byte[] boosted = new byte[pcmData.length];
        for (int i = 0; i < samples.length; i++) {
            double adjusted = samples[i] * gain;
            adjusted = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, adjusted));
            short finalSample = (short) adjusted;

            // Write little-endian bytes
            boosted[2*i] = (byte) (finalSample & 0xFF);
            boosted[2*i + 1] = (byte) ((finalSample >> 8) & 0xFF);
        }

        return boosted;
    }

    private void cleanup() {
        processingPool.shutdown();
        if (dataLine != null && dataLine.isOpen()) {
            dataLine.stop();
            dataLine.close();
        }
    }

    public synchronized void stop() {
        running = false;
        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                processingPool.shutdown();
            }
        }
    }
}
