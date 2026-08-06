package isaf.tfc.autolancamentosbackend.dto;

import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

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
}