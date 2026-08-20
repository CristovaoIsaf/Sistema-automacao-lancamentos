package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Auditoria C04 — uma versão anterior de um Lancamento, tal como estava
 * imediatamente antes de uma edição (ver LancamentoServiceImpl.atualizar).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LancamentoHistoricoDTO {

    private Long id;

    private LocalDate dataAnterior;

    private String descricaoAnterior;

    private List<LinhaLancamentoDTO> linhasAnteriores;

    private Long alteradoPor;

    private String alteradoPorNome;

    private LocalDateTime alteradoEm;
}
