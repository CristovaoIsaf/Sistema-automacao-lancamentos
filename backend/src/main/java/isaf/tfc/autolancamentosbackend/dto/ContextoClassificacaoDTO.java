package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Saída do Context Engine (Fase 3 do plano de 20 fases) — "contexto
 * relevante para classificação", agregado a partir de empresa, entidade e
 * histórico. Deliberadamente pequeno: não inclui o plano de contas nem as
 * regras contabilísticas (essas já vivem no código — pgc.py/
 * document_validation.py — não fazem sentido "duplicadas" aqui) nem
 * documentos inteiros do histórico, só um resumo compacto.
 *
 * "versao" cumpre o requisito de ser versionável — mesma disciplina já
 * usada nas versões de cache/validação do lado FastAPI (ver
 * VALIDATION_ENGINE_VERSION, AI_CACHE_VERSION).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContextoClassificacaoDTO {

    public static final String VERSAO = "TFC-2026-v1";

    private String versao;

    // Empresa (Fase 2) — CONTEXTO qualitativo do negócio.
    private String empresaAtividadeEconomica;
    private String empresaNaturezaNegocio;

    // Empresa — FACTO.
    private String empresaMoeda;
    private LocalDate exercicioAtualInicio;
    private LocalDate exercicioAtualFim;

    // Entidade de origem do documento (pode ser null — documento novo,
    // entidade ainda não resolvida).
    private Long entidadeId;
    private String entidadeNome;
    private String entidadeNif;
    private String entidadeTipo;

    // Histórico relevante — só os tipos de operação já vistos para esta
    // entidade nos últimos documentos, não os documentos/lançamentos
    // inteiros ("pequeno" e "relevante", não a base de dados toda).
    private List<String> historicoTiposOperacaoRecentes;
}
