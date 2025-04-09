package de.sven.bayer.speaking_llm.service;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
public class AudioPlayerService {
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private final Thread playerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AudioPlayerService() {
        playerThread = new Thread(this::processAudioQueue, "Audio-Player-Thread");
        playerThread.setDaemon(true);
        playerThread.start();
    }

    public void queueAudio(byte[] audioData) {
        try {
            audioQueue.put(audioData);
            System.out.println("Added audio to queue. Queue size: " + audioQueue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while queuing audio", e);
        }
    }

    public boolean isAudioFinished() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return audioQueue.isEmpty() && !running.get();
    }

    private void processAudioQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] audioData = audioQueue.take();
                playAudio(audioData);
                while (running.get()) {
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void playAudio(byte[] audioData) {
        running.set(true);
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(bais));

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    running.set(false);
                }
            });

            clip.start();
            System.out.println("Playing audio clip");

        } catch (Exception e) {
            System.err.println("Error playing audio:" + e);
            running.set(false);
        }
    }
}