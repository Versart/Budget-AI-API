package br.com.dio.budgetai.integration;

import br.com.dio.budgetai.ai.TransactionTools;
import br.com.dio.budgetai.domain.repository.AuditLogRepository;
import br.com.dio.budgetai.domain.repository.TransactionRepository;
import br.com.dio.budgetai.dto.AssistantTextRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.audio.speech.SpeechModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssistantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionTools transactionTools;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @MockitoBean
    private TranscriptionModel transcriptionModel;

    @MockitoBean
    private SpeechModel speechModel;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        auditLogRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/assistant/text deve processar comando de texto, acionar Tool Calling e persistir log de auditoria")
    void shouldProcessTextCommandAndPersistTransactionAndAudit() throws Exception {
        String command = "Registre uma despesa de 75 reais com farmácia";

        when(chatClient.prompt().user(command).tools(transactionTools).call().content())
                .thenAnswer(inv -> {
                    transactionTools.registerTransaction(
                            "Farmácia",
                            new BigDecimal("75.00"),
                            "EXPENSE",
                            "HEALTH",
                            "2026-11-08"
                    );
                    return "Registrei sua despesa de R$ 75,00 na categoria Saúde (Farmácia).";
                });

        AssistantTextRequest request = new AssistantTextRequest(command);

        mockMvc.perform(post("/api/assistant/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel", is("TEXT")))
                .andExpect(jsonPath("$.recognizedCommand", is(command)))
                .andExpect(jsonPath("$.assistantReply", is("Registrei sua despesa de R$ 75,00 na categoria Saúde (Farmácia).")));

        assertThat(transactionRepository.count()).isEqualTo(1L);
        assertThat(auditLogRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("POST /api/assistant/voice deve receber áudio, transcrever em texto, executar função via IA e auditar")
    void shouldProcessVoiceCommandEndToEnd() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio",
                "comando-voz.mp3",
                "audio/mpeg",
                new byte[]{1, 2, 3, 4, 5}
        );

        String transcribedText = "Recebi 4500 reais de salário hoje";
        AudioTranscription transcription = new AudioTranscription(transcribedText);
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(new AudioTranscriptionResponse(transcription));

        when(chatClient.prompt().user(transcribedText).tools(transactionTools).call().content())
                .thenAnswer(inv -> {
                    transactionTools.registerTransaction(
                            "Salário",
                            new BigDecimal("4500.00"),
                            "INCOME",
                            "SALARY",
                            "2026-11-08"
                    );
                    return "Receita de R$ 4.500,00 (Salário) adicionada com sucesso ao seu orçamento!";
                });

        mockMvc.perform(multipart("/api/assistant/voice").file(audioFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel", is("VOICE")))
                .andExpect(jsonPath("$.recognizedCommand", is(transcribedText)))
                .andExpect(jsonPath("$.assistantReply", is("Receita de R$ 4.500,00 (Salário) adicionada com sucesso ao seu orçamento!")));

        assertThat(transactionRepository.count()).isEqualTo(1L);

        mockMvc.perform(get("/api/assistant/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].channel", is("VOICE")));
    }

    @Test
    @DisplayName("POST /api/assistant/voice/audio-reply deve retornar resposta em áudio MP3 (Text-to-Speech)")
    void shouldReturnSynthesizedAudioFromVoiceCommand() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio",
                "saldo.mp3",
                "audio/mpeg",
                new byte[]{5, 4, 3, 2, 1}
        );

        String transcribedText = "Qual é o meu saldo?";
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(new AudioTranscriptionResponse(new AudioTranscription(transcribedText)));

        when(chatClient.prompt().user(transcribedText).tools(transactionTools).call().content())
                .thenReturn("Seu saldo atual é de zero reais.");

        byte[] fakeMp3 = new byte[]{73, 68, 51, 4, 0};
        when(speechModel.call(anyString())).thenReturn(fakeMp3);

        mockMvc.perform(multipart("/api/assistant/voice/audio-reply").file(audioFile))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(fakeMp3));
    }
}
