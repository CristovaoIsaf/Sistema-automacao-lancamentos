package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fase 6 do plano de 20 fases: espelha o shape devolvido por GET /pgc/contas
 * no FastAPI (ver services/pgc.py `plano_de_contas()`) — a única enumeração
 * do plano de contas deste projeto. `classe`/`subconta`/`natureza` podem ser
 * null (compatibilidade com o uso mais antigo do DTO, só código+nome).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContaDTO {

    private String codigo;

    private String nome;

    private String classe;

    private String subconta;

    private String natureza;

    public ContaDTO(String codigo, String nome) {
        this(codigo, nome, null, null, null);
    }
}
