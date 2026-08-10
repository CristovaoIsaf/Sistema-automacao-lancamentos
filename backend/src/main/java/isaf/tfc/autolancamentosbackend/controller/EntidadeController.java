package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.DocumentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.EntidadeDossieDTO;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.service.DocumentoEnriquecimentoService;
import isaf.tfc.autolancamentosbackend.service.PerfilEntidadeClient;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Entidades de origem dos documentos (clientes/fornecedores, ver
 * EntidadeService) — Fase 10 do plano de 20 fases: "criar visão de dossiê
 * da entidade". Só leitura (a criação/resolução de entidades acontece
 * implicitamente em EntidadeService.resolverOuCriar, durante a análise de
 * um documento — não há aqui um "criar entidade" manual).
 */
@RestController
@RequestMapping("/api/entidades")
public class EntidadeController {

    private final EntidadeRepository entidadeRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoEnriquecimentoService enriquecimentoService;
    private final PerfilEntidadeClient perfilEntidadeClient;

    public EntidadeController(EntidadeRepository entidadeRepository,
                               DocumentoRepository documentoRepository,
                               DocumentoEnriquecimentoService enriquecimentoService,
                               PerfilEntidadeClient perfilEntidadeClient) {
        this.entidadeRepository = entidadeRepository;
        this.documentoRepository = documentoRepository;
        this.enriquecimentoService = enriquecimentoService;
        this.perfilEntidadeClient = perfilEntidadeClient;
    }

    @GetMapping
    public ResponseEntity<List<Entidade>> listar() {
        return ResponseEntity.ok(entidadeRepository.findAll());
    }

    /**
     * Dossiê da entidade: os seus dados + todos os documentos arquivados
     * sob ela (mesmo enriquecimento — tipo/número/valor da Sugestao mais
     * recente — usado no arquivo completo em DocumentoController.listar()).
     *
     * @Transactional(readOnly = true) — DocumentoContabilistico.conteudo é
     * um @Lob; o Postgres exige uma transacção para ler large objects
     * ("Large Objects may not be used in auto-commit mode"), confirmado
     * ao vivo nesta sessão (500 sem esta anotação). Mesmo padrão já usado
     * em DocumentoController.listar()/buscar()/download().
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<EntidadeDossieDTO> dossie(@PathVariable Long id) {
        Entidade entidade = entidadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entidade não encontrada: " + id));

        List<DocumentoResponseDTO> documentos = enriquecimentoService.converterTodos(
                documentoRepository.findByEntidadeId(id));

        return ResponseEntity.ok(new EntidadeDossieDTO(
                entidade.getId(),
                entidade.getNome(),
                entidade.getNif(),
                entidade.getTipo() != null ? entidade.getTipo().name() : null,
                documentos,
                perfilEntidadeClient.obter(entidade.getNif())
        ));
    }
}
