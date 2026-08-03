package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Uma linha de partidas dobradas sugerida pela API de análise (pgc_ao),
 * ainda em formato texto tal como o FastAPI a devolve.
 */
@Data
@NoArgsConstructor
public class LinhaSugeridaDTO {

    private String conta;
    private String nome;
    private String debito;
    private String credito;
    private String descricao;
}
