package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.AnalisarDocumentoRequest;
import isaf.tfc.autolancamentosbackend.dto.AprovarSugestaoRequest;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.service.AnaliseContabilService;
import isaf.tfc.autolancamentosbackend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analises")
public class AnaliseController {

    private final AnaliseContabilService analiseContabilService;
    // Auditoria C06/C07 — rejeitar uma sugestão da IA nunca tinha nenhum
    // registo (ver AuditoriaService, comentário original: "Ficam FORA do
    // âmbito ... rejeição de sugestões").
    private final AuditLogService auditLogService;

    public AnaliseController(AnaliseContabilService analiseContabilService, AuditLogService auditLogService) {
        this.analiseContabilService = analiseContabilService;
        this.auditLogService = auditLogService;
    }

    /**
     * Passo 1: recebe o id de um DocumentoContabilistico já enviado (upload),
     * manda o ficheiro ao Gemini e grava uma Sugestao com estado PENDENTE.
     */
    // Auditoria C02: analisar/aprovar/rejeitar ficam reservados ao
    // Contabilista, tal como a criação manual (RN002/RN010, ver
    // LancamentoController.criar) — antes desta correção, o Administrador
    // conseguia oficializar lançamentos aprovando sugestões da IA, apesar
    // de essa mesma regra o proibir no caminho manual.
    @PreAuthorize("hasRole('CONTABILISTA')")
    @PostMapping
    public ResponseEntity<Sugestao> analisar(@RequestBody AnalisarDocumentoRequest request) {
        Sugestao sugestao = analiseContabilService.analisarDocumento(request.getDocumentoId());
        return ResponseEntity.ok(sugestao);
    }

    /**
     * Passo 2: aprova a Sugestao indicada, criando o Lancamento oficial
     * (origem AUTOMATICO) com a respetiva LinhaLancamento. `validadoPor`
     * vem do utilizador autenticado (JWT), nunca do cliente — antes era um
     * @RequestParam que o frontend enviava fixo a "1".  O corpo é opcional:
     * se vier preenchido, aprova exatamente as linhas revistas no ecrã
     * (LancamentoDiario.tsx); se não vier, mantém o comportamento antigo
     * (lê linhasJson da Sugestao tal como foi gravado na análise).
     */
    // Auditoria C02 — ver nota em analisar() acima.
    @PreAuthorize("hasRole('CONTABILISTA')")
    @PostMapping("/{id}/aprovar")
    public ResponseEntity<LancamentoResponseDTO> aprovar(@PathVariable Long id,
                                                           @AuthenticationPrincipal User user,
                                                           @RequestBody(required = false) AprovarSugestaoRequest request) {
        LancamentoResponseDTO lancamento = analiseContabilService.aprovarSugestao(id, user.getId(), request);
        return ResponseEntity.ok(lancamento);
    }

    /**
     * Anula uma Sugestao pendente — o contabilista decidiu que a proposta da
     * IA não deve virar lançamento nenhum. Não cria Lancamento.
     */
    // Auditoria C02 — ver nota em analisar() acima.
    @PreAuthorize("hasRole('CONTABILISTA')")
    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<Void> rejeitar(@PathVariable Long id, @AuthenticationPrincipal User user, HttpServletRequest httpRequest) {
        analiseContabilService.rejeitarSugestao(id);
        auditLogService.registar(user, "REJEITAR_SUGESTAO", "Sugestao", id,
                AuditLogService.SUCESSO, null, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}