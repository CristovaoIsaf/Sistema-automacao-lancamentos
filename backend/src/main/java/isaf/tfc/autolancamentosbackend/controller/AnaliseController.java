package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.AnalisarDocumentoRequest;
import isaf.tfc.autolancamentosbackend.dto.AprovarSugestaoRequest;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.service.AnaliseContabilService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analises")
public class AnaliseController {

    private final AnaliseContabilService analiseContabilService;

    public AnaliseController(AnaliseContabilService analiseContabilService) {
        this.analiseContabilService = analiseContabilService;
    }

    /**
     * Passo 1: recebe o id de um DocumentoContabilistico já enviado (upload),
     * manda o ficheiro ao Gemini e grava uma Sugestao com estado PENDENTE.
     */
    // RN010: escrita fica fora do Auditor (ver DocumentoController.upload).
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CONTABILISTA')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CONTABILISTA')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CONTABILISTA')")
    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<Void> rejeitar(@PathVariable Long id) {
        analiseContabilService.rejeitarSugestao(id);
        return ResponseEntity.noContent().build();
    }
}