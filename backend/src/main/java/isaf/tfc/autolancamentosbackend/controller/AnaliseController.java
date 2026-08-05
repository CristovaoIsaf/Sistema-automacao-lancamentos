package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.AnalisarDocumentoRequest;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.service.AnaliseContabilService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * (origem AUTOMATICO) com a respetiva LinhaLancamento.
     */
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CONTABILISTA')")
    @PostMapping("/{id}/aprovar")
    public ResponseEntity<LancamentoResponseDTO> aprovar(@PathVariable Long id,
                                                           @RequestParam Long validadoPor) {
        LancamentoResponseDTO lancamento = analiseContabilService.aprovarSugestao(id, validadoPor);
        return ResponseEntity.ok(lancamento);
    }
}