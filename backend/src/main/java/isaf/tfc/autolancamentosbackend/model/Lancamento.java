package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Aditivo — só relevante para origem AUTOMATICO: distingue "aprovado tal
    // como a IA sugeriu" (false/null) de "contabilista alterou as linhas
    // antes de aprovar" (true). Lançamentos manuais ficam sempre null.
    private Boolean editadoManualmente;

    // Aditivo (Fase 15 do plano de 20 fases — "consultar auditoria"):
    // "data" acima é a data CONTABILÍSTICA do lançamento (editável pelo
    // contabilista, ex. lançar hoje um documento de ontem) — nunca serviu
    // para saber QUANDO o registo foi mesmo criado no sistema, o que um
    // log de auditoria exige. Preenchido automaticamente (@PrePersist,
    // mesmo padrão já usado em Sugestao.aoCriar()/DocumentoContabilistico.
    // aoCriar()), nunca editável. Lançamentos antigos (antes desta fase)
    // ficam com null — a auditoria trata isso como "data desconhecida".
    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    // Aditivo (Fase 16 do plano de 20 fases — "auditor: alterações"):
    // até aqui não havia nenhum rasto de quando/quem editou (PUT) ou
    // cancelou (POST /cancelar) um lançamento depois de criado — a
    // auditoria da Fase 15 só via a criação. atualizadoEm fica a null
    // enquanto o lançamento nunca for alterado; alteradoPor guarda só a
    // ÚLTIMA alteração (não um histórico completo — a mesma simplificação
    // já assumida em criadoEm/AuditoriaService).
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "alterado_por")
    private Long alteradoPor;

    @PrePersist
    public void aoCriar() {
        if (this.criadoEm == null) {
            this.criadoEm = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    @OneToMany(
            mappedBy = "lancamento",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<LinhaLancamento> linhas = new ArrayList<>();
}
