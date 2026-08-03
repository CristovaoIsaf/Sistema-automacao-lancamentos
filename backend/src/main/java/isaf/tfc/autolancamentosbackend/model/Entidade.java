package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade de origem de um documento (cliente ou fornecedor), inferida a
 * partir do NIF/nome extraídos pela análise (FastAPI) — T2: arquivo por
 * entidade. Um NIF em branco/desconhecido cai sempre na mesma entidade
 * partilhada "Sem identificação" (ver EntidadeService).
 */
@Entity
@Table(name = "entidades_documento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String nif;

    @Enumerated(EnumType.STRING)
    private TipoEntidade tipo;
}
