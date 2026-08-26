package isaf.tfc.autolancamentosbackend.dto;

import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fase 9 do plano de 20 fases ("histórico deve permitir filtro por
 * ... documento; entidade; ... utilizador"): ganhou documentoId/
 * entidadeId/entidadeNome/validadoPor/validadoPorNome — antes desta fase o
 * histórico só sabia mostrar data/descrição/estado/origem/linhas, mesmo a
 * entidade e quem validou já estando gravados em Lancamento (validadoPor)
 * ou alcançáveis via Lancamento.sugestaoId → Sugestao.documentoId →
 * DocumentoContabilistico.entidadeId. Ver LancamentoEnriquecimentoService,
 * o único ponto que preenche estes campos (para MANUAL e AUTOMATICO).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LancamentoResponseDTO {

    private Long id;

    private LocalDate data;

    private String descricao;

    private EstadoLancamento estado;

    private OrigemLancamento origem;

    private Boolean editadoManualmente;

    private List<LinhaLancamentoDTO> linhas;

    // Modelação de IVA no domínio Java — ver Lancamento.valorIva /
    // PartidasDobradas.calcularValorIva.
    private BigDecimal valorIva;

    private Long documentoId;

    private Long entidadeId;

    private String entidadeNome;

    private Long validadoPor;

    private String validadoPorNome;

    // Auditoria C01/C03 — ver LancamentoEnriquecimentoService.construir().
    private Long criadoPor;

    private String criadoPorNome;

    private String motivoCancelamento;

    private Long cancelamentoSolicitadoPor;

    private String cancelamentoSolicitadoPorNome;

    private Long estornoDeId;
}
