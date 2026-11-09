package br.com.dio.budgetai.service;

import br.com.dio.budgetai.ai.TransactionTools;
import br.com.dio.budgetai.domain.model.AuditLog;
import br.com.dio.budgetai.domain.repository.AuditLogRepository;
import br.com.dio.budgetai.dto.AssistantResponse;
import br.com.dio.budgetai.exception.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssistantService {

    private final ChatClient chatClient;
    private final TransactionTools transactionTools;
    private final AudioService audioService;
    private final AuditLogRepository auditLogRepository;

    public AssistantService(
            ChatClient chatClient,
            TransactionTools transactionTools,
            AudioService audioService,
            AuditLogRepository auditLogRepository
    ) {
        this.chatClient = chatClient;
        this.transactionTools = transactionTools;
        this.audioService = audioService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AssistantResponse processTextCommand(String userCommand) {
        if (userCommand == null || userCommand.isBlank()) {
            throw new BusinessException("O comando de texto não pode estar vazio");
        }

        String aiReply = executeChatWithTools(userCommand.trim());
        AuditLog savedAudit = auditLogRepository.save(new AuditLog("TEXT", userCommand.trim(), aiReply));

        return new AssistantResponse(
                "TEXT",
                userCommand.trim(),
                aiReply,
                savedAudit.getExecutedAt() != null ? savedAudit.getExecutedAt() : LocalDateTime.now()
        );
    }

    @Transactional
    public AssistantResponse processVoiceCommand(MultipartFile audioFile) {
        String transcribedText = audioService.transcribeAudio(audioFile);
        String aiReply = executeChatWithTools(transcribedText);
        AuditLog savedAudit = auditLogRepository.save(new AuditLog("VOICE", transcribedText, aiReply));

        return new AssistantResponse(
                "VOICE",
                transcribedText,
                aiReply,
                savedAudit.getExecutedAt() != null ? savedAudit.getExecutedAt() : LocalDateTime.now()
        );
    }

    @Transactional
    public byte[] processVoiceCommandWithAudioReply(MultipartFile audioFile) {
        AssistantResponse response = processVoiceCommand(audioFile);
        return audioService.synthesizeSpeech(response.assistantReply());
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listAuditLogs() {
        return auditLogRepository.findAllByOrderByExecutedAtDesc();
    }

    private String executeChatWithTools(String command) {
        String content = chatClient.prompt()
                .user(command)
                .tools(transactionTools)
                .call()
                .content();
        return content != null ? content : "Comando processado com sucesso.";
    }
}
