package br.com.dio.budgetai.unit;

import br.com.dio.budgetai.exception.BusinessException;
import br.com.dio.budgetai.service.AudioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.openai.audio.speech.SpeechModel;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioServiceTest {

    @Mock
    private TranscriptionModel transcriptionModel;

    @Mock
    private SpeechModel speechModel;

    @InjectMocks
    private AudioService audioService;

    @Test
    @DisplayName("Deve transcrever arquivo de áudio válido em texto")
    void shouldTranscribeValidAudioFile() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio",
                "comando.mp3",
                "audio/mpeg",
                new byte[]{1, 2, 3, 4}
        );

        AudioTranscription transcription = new AudioTranscription("Registre um gasto de 80 reais no mercado");
        AudioTranscriptionResponse response = new AudioTranscriptionResponse(transcription);

        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class))).thenReturn(response);

        String result = audioService.transcribeAudio(audioFile);

        assertThat(result).isEqualTo("Registre um gasto de 80 reais no mercado");
        verify(transcriptionModel).call(any(AudioTranscriptionPrompt.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando arquivo de áudio estiver vazio")
    void shouldThrowExceptionWhenAudioFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("audio", "vazio.mp3", "audio/mpeg", new byte[0]);

        assertThatThrownBy(() -> audioService.transcribeAudio(emptyFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("arquivo de áudio é obrigatório");
    }

    @Test
    @DisplayName("Deve sintetizar voz a partir de texto válido")
    void shouldSynthesizeSpeechFromValidText() {
        byte[] expectedAudio = new byte[]{10, 20, 30};
        when(speechModel.call("Seu saldo atual é de 1500 reais.")).thenReturn(expectedAudio);

        byte[] result = audioService.synthesizeSpeech("Seu saldo atual é de 1500 reais.");

        assertThat(result).isEqualTo(expectedAudio);
        verify(speechModel).call("Seu saldo atual é de 1500 reais.");
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando texto para síntese de voz estiver em branco")
    void shouldThrowExceptionWhenSpeechTextIsBlank() {
        assertThatThrownBy(() -> audioService.synthesizeSpeech("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("texto para síntese de voz não pode estar vazio");
    }
}
