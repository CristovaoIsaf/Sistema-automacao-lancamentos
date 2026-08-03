package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Apesar do nome (herdado do tipo já definido no frontend), representa um
 * Lancamento recente — é o que a tabela "Lançamentos Recentes" do Dashboard
 * consome.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoRecenteDTO {

    private String id;

    private String nome;

    private String tipo;

    private String data;

    private String estado;

    private BigDecimal valor;

    private String origem;
}
