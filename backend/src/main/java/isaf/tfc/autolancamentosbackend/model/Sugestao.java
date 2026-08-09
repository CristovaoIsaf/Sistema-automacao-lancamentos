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

    // Confiança (0-100) da classificação — já vinha em AnaliseResponse.confianca
    // desde sempre, mas nunca tinha sido persistida aqui; ficava
    // silenciosamente perdida antes de chegar ao frontend (Fase 8 do plano
    // de 20 fases: "mostrar imediatamente ... confiança" exige guardá-la).
    // Aditiva: sugestões antigas ficam com null.
    private Integer confianca;

    // Resultado do motor de validação determinística (Fase 3 — ver
    // fastapi/app/services/document_validation.py), serializado em JSON,
    // tal como linhasJson. Aditiva: sugestões antigas ficam com null, o
    // frontend trata isso como "sem informação de validação".
    @Column(name = "validacao_json", columnDefinition = "TEXT")
    private String validacaoJson;

    // Contexto usado para esta classificação (Fase 3 — Context Engine, ver
    // ContextoClassificacaoService), serializado em JSON, tal como
    // validacaoJson. Puramente para RASTREABILIDADE nesta fase — a
    // classificação em si ainda não o consome (isso é a Fase 5). Aditiva:
    // sugestões antigas ficam com null.
    @Column(name = "contexto_json", columnDefinition = "TEXT")
    private String contextoJson;

    // Fase 4 — "contextualização assistida" (ver
    // PerguntaContextualizacaoDTO), serializada em JSON, tal como
    // validacaoJson. null quando a classificação já foi confiante e não
    // houve nenhuma pergunta a fazer.
    @Column(name = "pergunta_contextualizacao_json", columnDefinition = "TEXT")
    private String perguntaContextualizacaoJson;

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