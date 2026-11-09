package br.com.dio.budgetai.controller;

import br.com.dio.budgetai.domain.model.AuditLog;
import br.com.dio.budgetai.dto.AssistantResponse;
import br.com.dio.budgetai.dto.AssistantTextRequest;
import br.com.dio.budgetai.service.AssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
@Tag(name = "Assistente IA de Orçamento", description = "Comandos inteligentes por voz e texto com Spring AI e Tool Calling")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/text")
    @Operation(summary = "Processa um comando financeiro em texto usando IA e executa funções reais (Tool Calling)")
    public ResponseEntity<AssistantResponse> processTextCommand(@Valid @RequestBody AssistantTextRequest request) {
        return ResponseEntity.ok(assistantService.processTextCommand(request.command()));
    }

    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Recebe um arquivo de áudio, transcreve em texto, executa a função via IA e retorna a resposta em JSON")
    public ResponseEntity<AssistantResponse> processVoiceCommand(@RequestParam("audio") MultipartFile audioFile) {
        return ResponseEntity.ok(assistantService.processVoiceCommand(audioFile));
    }

    @PostMapping(
            value = "/voice/audio-reply",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "audio/mpeg"
    )
    @Operation(summary = "Recebe um arquivo de áudio, processa o comando via IA e devolve a resposta sintetizada em voz (MP3)")
    public ResponseEntity<byte[]> processVoiceCommandWithAudioReply(@RequestParam("audio") MultipartFile audioFile) {
        byte[] speechBytes = assistantService.processVoiceCommandWithAudioReply(audioFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"assistant-reply.mp3\"")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(speechBytes);
    }

    @GetMapping("/audit")
    @Operation(summary = "Consulta o histórico de auditoria de todos os comandos processados pelo Assistente de IA")
    public ResponseEntity<List<AuditLog>> listAuditLogs() {
        return ResponseEntity.ok(assistantService.listAuditLogs());
    }
}
