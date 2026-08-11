package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fase 15 do plano de 20 fases — "visualizar pendências": uma Sugestao
 * ainda PENDENTE (ver Sugestao.estado), à espera de revisão pelo
 * contabilista. Só os campos necessários para o Administrador identificar
 * e navegar até ela — os dados completos já vêm de GET /analises (não
 * duplicados aqui).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendenciaDTO {

    private Long sugestaoId;

    private Long documentoId;

    private String descricao;

    private String entidade;

    private String valor;

    private LocalDateTime dataCriacao;
}
