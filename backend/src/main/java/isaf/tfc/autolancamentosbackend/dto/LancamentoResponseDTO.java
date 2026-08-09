package isaf.tfc.autolancamentosbackend.dto;

import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private Long documentoId;

    private Long entidadeId;

    private String entidadeNome;

    private Long validadoPor;

    private String validadoPorNome;
}
