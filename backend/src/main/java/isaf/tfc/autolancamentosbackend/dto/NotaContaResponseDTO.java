package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotaContaResponseDTO {

    private String conta;

    // Fase 14 do plano de 20 fases — "notas às contas": nome/natureza da
    // conta (mesma fonte que o plano de contas real, ver PlanoContasClient)
    // necessários para redigir a nota em prosa coerente (ex. "conta 32 —
    // Fornecedores" em vez de só "conta 32", e "saldo credor" vs "devedor"
    // consoante a natureza). Podem ser null se a conta não constar do
    // plano de contas devolvido pelo FastAPI.
    private String nomeConta;

    private String natureza;

    private LocalDate inicio;

    private LocalDate fim;

    private List<GrupoEntidadeDTO> porEntidade;

    private BigDecimal totalDebito;

    private BigDecimal totalCredito;

    // Diferença aritmética débito-crédito — a leitura como "saldo devedor" ou
    // "saldo credor" depende da natureza da conta consultada (não codificada
    // aqui; ver Decreto 82/01 para a natureza de cada conta).
    private BigDecimal saldo;
}
