package br.com.dio.budgetai.service;

import br.com.dio.budgetai.exception.BusinessException;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.openai.audio.speech.SpeechModel;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AudioService {

    private final TranscriptionModel transcriptionModel;
    private final SpeechModel speechModel;

    public AudioService(TranscriptionModel transcriptionModel, SpeechModel speechModel) {
        this.transcriptionModel = transcriptionModel;
        this.speechModel = speechModel;
    }

    public String transcribeAudio(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException("O arquivo de áudio é obrigatório e não pode estar vazio");
        }

        Resource audioResource = audioFile.getResource();
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource);
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);

        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BusinessException("Não foi possível transcrever o áudio enviado");
        }

        String text = response.getResult().getOutput().trim();
        if (text.isBlank()) {
            throw new BusinessException("O áudio enviado não contém fala reconhecível");
        }
        return text;
    }

    public byte[] synthesizeSpeech(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("O texto para síntese de voz não pode estar vazio");
        }
        return speechModel.call(text);
    }
}
