package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContextoClassificacaoDTO;
import isaf.tfc.autolancamentosbackend.dto.EmpresaDTO;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Context Engine (Fase 3 do plano de 20 fases) — camada responsável por
 * agregar empresa + entidade + histórico relevante numa única estrutura
 * pequena e rastreável ("contexto relevante para classificação"), ANTES de
 * qualquer decisão de classificação.
 *
 * Não decide nada sozinho e não chama IA nenhuma — só junta factos e
 * contexto já existentes (nunca inventa nada, ver regra explícita da
 * Fase 2: "o contexto NÃO deve ser inventado pela IA"). Quem consome este
 * contexto para realmente influenciar uma classificação é a Fase 5.
 */
@Service
public class ContextoClassificacaoService {

    // Nº de sugestões recentes da entidade consideradas no histórico —
    // mantém o contexto "pequeno" mesmo para uma entidade com muitos
    // documentos.
    private static final int LIMITE_HISTORICO = 5;

    private final EmpresaService empresaService;
    private final EntidadeRepository entidadeRepository;
    private final SugestaoRepository sugestaoRepository;

    public ContextoClassificacaoService(
            EmpresaService empresaService,
            EntidadeRepository entidadeRepository,
            SugestaoRepository sugestaoRepository
    ) {
        this.empresaService = empresaService;
        this.entidadeRepository = entidadeRepository;
        this.sugestaoRepository = sugestaoRepository;
    }

    /**
     * Constrói o contexto para uma entidade já identificada. `entidadeId`
     * pode ser null (documento novo, entidade ainda não resolvida) — nesse
     * caso o contexto sai só com os dados da empresa, sem histórico.
     */
    public ContextoClassificacaoDTO construirContexto(Long entidadeId) {
        EmpresaDTO empresa = empresaService.obterEmpresa();
        Entidade entidade = entidadeId != null
                ? entidadeRepository.findById(entidadeId).orElse(null)
                : null;

        List<String> historico = entidade != null
                ? sugestaoRepository.findRecentesPorEntidade(entidadeId, PageRequest.of(0, LIMITE_HISTORICO))
                        .stream()
                        .map(Sugestao::getTipoDocumento)
                        .filter(Objects::nonNull)
                        .toList()
                : List.of();

        return new ContextoClassificacaoDTO(
                ContextoClassificacaoDTO.VERSAO,
                empresa.getAtividadeEconomica(),
                empresa.getNaturezaNegocio(),
                empresa.getMoeda(),
                empresa.getExercicioAtualInicio(),
                empresa.getExercicioAtualFim(),
                entidade != null ? entidade.getId() : null,
                entidade != null ? entidade.getNome() : null,
                entidade != null ? entidade.getNif() : null,
                entidade != null && entidade.getTipo() != null ? entidade.getTipo().name() : null,
                historico
        );
    }
}
