package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lancamento")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sugestaoId;

    private Long validadoPor;

    private LocalDate data;

    private String descricao;

    @Enumerated(EnumType.STRING)
    private EstadoLancamento estado;

    @Enumerated(EnumType.STRING)
    private OrigemLancamento origem;
    @OneToMany(
            mappedBy = "lancamento",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<LinhaLancamento> linhas = new ArrayList<>();
}
