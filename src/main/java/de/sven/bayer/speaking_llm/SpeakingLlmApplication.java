package de.sven.bayer.speaking_llm;

import de.sven.bayer.speaking_llm.component.audio.AudioRecorder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpeakingLlmApplication {
	private final AudioRecorder audioRecorder;

	public SpeakingLlmApplication(AudioRecorder audioRecorder) {
		this.audioRecorder = audioRecorder;
	}

	public static void main(String[] args) {
		SpringApplication.run(SpeakingLlmApplication.class, args);
	}

	@Bean
	public CommandLineRunner startAudioRecorder() {
		return args -> {
			Runtime.getRuntime().addShutdownHook(new Thread(audioRecorder::stop));
			audioRecorder.start();
		};
	}
}
