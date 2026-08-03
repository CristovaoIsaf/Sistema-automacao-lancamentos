package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Table(name = "linha_lancamento")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LinhaLancamento {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String conta;


    private BigDecimal debito;


    private BigDecimal credito;


    private String descricao;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lancamento_id")
    private Lancamento lancamento;

}
