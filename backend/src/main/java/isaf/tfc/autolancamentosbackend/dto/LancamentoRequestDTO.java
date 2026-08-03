package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LancamentoRequestDTO {

    private LocalDate data;

    private String descricao;


    private List<LinhaLancamentoDTO> linhas;

}
