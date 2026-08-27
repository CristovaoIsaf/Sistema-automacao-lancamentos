package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
// Auditoria de performance: GET /api/lancamentos e todos os relatórios
// (Balancete/DRE/Balanço/Fluxo de Caixa/Livro Razão) filtram por
// findByDataBetween sem nenhum índice em "data" — table scan completo em
// cada pedido à medida que o histórico cresce.
@Table(name = "lancamento", indexes = @Index(name = "idx_lancamento_data", columnList = "data"))
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

    // Modelação de IVA no domínio Java — soma das linhas cuja conta é de
    // IVA (34.5.1/34.5.2, ver PartidasDobradas.calcularValorIva). Calculado
    // sempre que as linhas do lançamento são definidas/substituídas (manual,
    // aprovação de sugestão, edição, estorno) — nunca lido diretamente do
    // FastAPI. ZERO quando não há linha de IVA, nunca null.
    @Column(name = "valor_iva")
    private BigDecimal valorIva;

    // columnDefinition explícito (Auditoria C01/C03 — descoberto num teste
    // HTTP ao vivo antes desta correção): sem isto, o Hibernate 7 gera por
    // omissão uma CHECK constraint em BD listando os valores do enum NO
    // MOMENTO em que a coluna foi criada pela primeira vez — spring.jpa.
    // hibernate.ddl-auto=update nunca a atualiza depois, por isso
    // acrescentar CANCELAMENTO_PENDENTE ao enum Java rebentava com
    // "violates check constraint lancamento_estado_check" em qualquer BD
    // já existente (esta em dev incluída — constraint já removida
    // manualmente aqui). Isto evita o Hibernate voltar a gerar uma.
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255)")
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

    // Auditoria C01: até aqui não havia nenhum campo que guardasse quem
    // CRIOU um lançamento manual separado de quem o VALIDOU — as duas
    // coisas eram sempre a mesma pessoa, na mesma chamada
    // (criarLancamentoManual gravava criadoPor diretamente em
    // validadoPor). Só relevante para origem MANUAL; AUTOMATICO continua
    // sem criador humano direto (a origem é a Sugestao da IA — ver
    // sugestaoId).
    @Column(name = "criado_por")
    private Long criadoPor;

    // Auditoria C03: pedido de anulação de um lançamento VALIDADO —
    // preenchidos quando estado = CANCELAMENTO_PENDENTE, limpos de novo
    // (para null) se o pedido for rejeitado. motivoCancelamento fica
    // preenchido mesmo depois de aprovado (CANCELADO), como registo do
    // porquê.
    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;

    @Column(name = "cancelamento_solicitado_por")
    private Long cancelamentoSolicitadoPor;

    // Auditoria C03: quando este Lancamento É o estorno de outro (gerado
    // automaticamente ao aprovar um pedido de anulação — ver
    // LancamentoServiceImpl.aprovarCancelamento), aponta para o id do
    // Lancamento original revertido. null em todos os outros casos.
    @Column(name = "estorno_de_id")
    private Long estornoDeId;

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
