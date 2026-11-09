package br.com.dio.budgetai.unit;

import br.com.dio.budgetai.ai.TransactionTools;
import br.com.dio.budgetai.domain.model.AuditLog;
import br.com.dio.budgetai.domain.repository.AuditLogRepository;
import br.com.dio.budgetai.dto.AssistantResponse;
import br.com.dio.budgetai.exception.BusinessException;
import br.com.dio.budgetai.service.AssistantService;
import br.com.dio.budgetai.service.AudioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private TransactionTools transactionTools;

    @Mock
    private AudioService audioService;

    @Mock
    private AuditLogRepository auditLogRepository;

    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        assistantService = new AssistantService(chatClient, transactionTools, audioService, auditLogRepository);
    }

    @Test
    @DisplayName("Deve processar comando em texto via ChatClient, acionar Tools e registrar auditoria")
    void shouldProcessTextCommandAndSaveAuditLog() {
        String command = "Adicione uma despesa de 95 reais com almoço";
        String expectedReply = "Despesa de R$ 95,00 (Alimentação - Almoço) registrada com sucesso!";

        when(chatClient.prompt().user(command).tools(transactionTools).call().content())
                .thenReturn(expectedReply);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AssistantResponse response = assistantService.processTextCommand(command);

        assertThat(response.channel()).isEqualTo("TEXT");
        assertThat(response.recognizedCommand()).isEqualTo(command);
        assertThat(response.assistantReply()).isEqualTo(expectedReply);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Deve processar comando de voz: transcrever áudio, consultar IA e salvar auditoria")
    void shouldProcessVoiceCommandAndSaveAuditLog() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio",
                "voz.mp3",
                "audio/mpeg",
                new byte[]{1, 2, 3}
        );
        String transcribed = "Qual é o meu saldo atual?";
        String aiReply = "Seu saldo atual é de R$ 2.400,00.";

        when(audioService.transcribeAudio(audioFile)).thenReturn(transcribed);
        when(chatClient.prompt().user(transcribed).tools(transactionTools).call().content())
                .thenReturn(aiReply);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AssistantResponse response = assistantService.processVoiceCommand(audioFile);

        assertThat(response.channel()).isEqualTo("VOICE");
        assertThat(response.recognizedCommand()).isEqualTo(transcribed);
        assertThat(response.assistantReply()).isEqualTo(aiReply);
        verify(audioService).transcribeAudio(audioFile);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Deve processar comando de voz e gerar resposta sintetizada em áudio (MP3)")
    void shouldProcessVoiceCommandAndReturnSynthesizedAudio() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio",
                "voz.mp3",
                "audio/mpeg",
                new byte[]{1, 2, 3}
        );
        String transcribed = "Quanto gastei hoje?";
        String aiReply = "Hoje você gastou R$ 120,00.";
        byte[] mp3Output = new byte[]{9, 8, 7, 6};

        when(audioService.transcribeAudio(audioFile)).thenReturn(transcribed);
        when(chatClient.prompt().user(transcribed).tools(transactionTools).call().content())
                .thenReturn(aiReply);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(audioService.synthesizeSpeech(aiReply)).thenReturn(mp3Output);

        byte[] result = assistantService.processVoiceCommandWithAudioReply(audioFile);

        assertThat(result).isEqualTo(mp3Output);
        verify(audioService).synthesizeSpeech(aiReply);
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando comando de texto for vazio")
    void shouldThrowExceptionWhenTextCommandIsBlank() {
        assertThatThrownBy(() -> assistantService.processTextCommand("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("comando de texto não pode estar vazio");
    }
}
