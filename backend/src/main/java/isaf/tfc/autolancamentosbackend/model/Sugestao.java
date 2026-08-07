package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sugestao")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Sugestao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Aditivo (T2 — rastreabilidade Documento → Sugestao → Lancamento).
    @Column(name = "documento_id")
    private Long documentoId;

    @Column(name = "tipo_documento")
    private String tipoDocumento;

    @Column(name = "categoria_contabil")
    private String categoriaContabil;

    // Categoria do plano de contas (Vendas/Compras/... — ver
    // CategoriaContaController), inferida a partir do tipoDocumento pelo
    // FastAPI (pgc_ao.categoria_do_tipo). Aditivo — sugestões antigas ficam
    // com este campo a null, o frontend já trata isso como "sem categoria".
    private String categoria;

    // Valor devolvido pelo Gemini em formato texto (ex: "150.000,00 Kz")
    private String valor;

    // Nome/NIF da entidade (fornecedor numa compra, cliente numa venda) —
    // já vem tratado por regex do FastAPI (regex_extract.py), nunca
    // "inventado" pela IA. Aditivo — sugestões antigas ficam com null.
    private String entidade;

    private String nif;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "texto_original", columnDefinition = "TEXT")
    private String textoOriginal;

    // Linhas de partidas dobradas sugeridas pela API de análise (pgc_ao),
    // serializadas em JSON. Guardadas aqui até a Sugestao ser aprovada,
    // altura em que passam a LinhaLancamento reais.
    @Column(name = "linhas_json", columnDefinition = "TEXT")
    private String linhasJson;

    // Trechos do Decreto 82/01 (RAG) que fundamentaram esta sugestão — coluna
    // aditiva, fica vazia para sugestões antigas ou vindas do fallback por regras.
    @Column(name = "fundamentacao", columnDefinition = "TEXT")
    private String fundamentacao;

    @Enumerated(EnumType.STRING)
    private EstadoSugestao estado;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    // Preenchido só depois de aprovada, referenciando o Lancamento criado
    @Column(name = "lancamento_id")
    private Long lancamentoId;

    @PrePersist
    public void aoCriar() {
        this.dataCriacao = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoSugestao.PENDENTE;
        }
    }
}