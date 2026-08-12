package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.CorrigirClassificacaoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.DocumentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.RenomearDocumentoRequestDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import isaf.tfc.autolancamentosbackend.service.DocumentoEnriquecimentoService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/documentos")
public class DocumentoController {

    private final DocumentoRepository documentoRepository;
    private final EntidadeRepository entidadeRepository;
    private final SugestaoRepository sugestaoRepository;
    private final LancamentoRepository lancamentoRepository;
    private final DocumentoEnriquecimentoService enriquecimentoService;
    private final TransactionTemplate novaTransacao;

    public DocumentoController(DocumentoRepository documentoRepository,
                                EntidadeRepository entidadeRepository,
                                SugestaoRepository sugestaoRepository,
                                LancamentoRepository lancamentoRepository,
                                DocumentoEnriquecimentoService enriquecimentoService,
                                PlatformTransactionManager transactionManager) {
        this.documentoRepository = documentoRepository;
        this.entidadeRepository = entidadeRepository;
        this.sugestaoRepository = sugestaoRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.enriquecimentoService = enriquecimentoService;

        // PROPAGATION_REQUIRES_NEW — usado só para o save() em upload(): se
        // a constraint UNIQUE em hash_conteudo rebentar (dois uploads
        // simultâneos do mesmo ficheiro), só esta transacção isolada é
        // anulada. Com o save() dentro da mesma transacção que a leitura
        // de pré-verificação (@Transactional no método upload), apanhar a
        // excepção não chega — o Spring já marca a transacção inteira como
        // rollback-only antes do catch, e o commit final rebenta com
        // UnexpectedRollbackException (confirmado num teste real de corrida
        // nesta sessão).
        this.novaTransacao = new TransactionTemplate(transactionManager);
        this.novaTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Upload de um documento (PDF ou imagem). Guarda os bytes no banco.
     * O ficheiro fica pronto para ser analisado via POST /analises com o documentoId devolvido aqui.
     *
     * Rejeita duplicados: dois uploads do mesmo ficheiro (mesmo hash SHA-256
     * dos bytes) originariam duas sugestões/lançamentos independentes para o
     * mesmo documento — risco contabilístico real (despesa/receita
     * duplicada), não só um incómodo de UI. Verificação global (não só do
     * utilizador que carrega), porque o risco existe independentemente de
     * quem submeteu o documento.
     */
    // RN010: escrita de documentos fica fora do Auditor (só leitura para
    // esse papel — ver GET /documentos e /documentos/{id}/download).
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CONTABILISTA')")
    @PostMapping(consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] conteudo = file.getBytes();
        String hash = calcularHash(conteudo);

        Optional<DocumentoContabilistico> existente = documentoRepository.findByHashConteudo(hash);
        if (existente.isPresent()) {
            return conflitoDuplicado(existente.get());
        }

        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setNomeFicheiro(file.getOriginalFilename());
        documento.setTipoConteudo(file.getContentType());
        documento.setConteudo(conteudo);
        documento.setHashConteudo(hash);
        documento.setUserId(user.getId());

        try {
            // Transacção isolada (REQUIRES_NEW) — ver comentário no
            // construtor sobre porque o save() não pode partilhar a
            // transacção da verificação findByHashConteudo acima.
            DocumentoContabilistico salvo = novaTransacao.execute(status -> documentoRepository.save(documento));
            return ResponseEntity.ok(salvo);
        } catch (DataIntegrityViolationException e) {
            // Corrida: dois uploads simultâneos do mesmo ficheiro passam
            // ambos pela verificação findByHashConteudo acima antes de
            // qualquer um gravar — só a constraint UNIQUE em hash_conteudo
            // (ver DocumentoContabilistico) apanha isto. Depois de uma
            // violação de integridade a transacção já está abortada do
            // lado do Postgres, por isso não se reconsulta a BD aqui — a
            // mensagem fica genérica; o caso comum, sem corrida, já teve a
            // mensagem detalhada acima.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Este documento já foi carregado anteriormente (por outro pedido em simultâneo)."
            ));
        }
    }

    private ResponseEntity<Map<String, Object>> conflitoDuplicado(DocumentoContabilistico anterior) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", "Este documento já foi carregado anteriormente (\""
                        + anterior.getNomeFicheiro() + "\", ID " + anterior.getId()
                        + ", em " + anterior.getDataUpload() + ")."
        ));
    }

    private String calcularHash(byte[] conteudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(conteudo);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }

    /**
     * Lista TODOS os documentos da empresa (sem o conteudo do ficheiro, que
     * continua disponível em GET /documentos/{id}). Inclui entidade,
     * estado (Pendente/Analisado/Aprovado/Rejeitado) e, quando já
     * analisado, tipo/número/valor — para a página de Arquivo pesquisar e
     * agrupar os documentos (Fase 10 do plano de 20 fases).
     *
     * Antes desta fase filtrava por findByUserId(user.getId()) — um
     * Auditor (que nunca faz upload, RN010) via sempre um arquivo vazio, e
     * um Contabilista não via os documentos carregados por outro
     * contabilista da mesma empresa. Contradizia a própria regra desta
     * fase: "a identidade documental não depende do contabilista;
     * contabilista = autoria/auditoria" — o documento pertence à empresa
     * (este projecto é single-tenant), não a quem o carregou.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<DocumentoResponseDTO>> listar(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(enriquecimentoService.converterTodos(documentoRepository.findAll()));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<DocumentoContabilistico> buscar(@PathVariable Long id) {
        return documentoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Renomeia o ficheiro no arquivo — o nome original do upload nem
     * sempre é útil para encontrar o documento depois (ex: "IMG_2049.jpg").
     */
    // RN010: escrita fica fora do Auditor, mesma regra do upload.
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CONTABILISTA')")
    @PatchMapping("/{id}/nome")
    @Transactional
    public ResponseEntity<DocumentoResponseDTO> renomear(
            @PathVariable Long id,
            @RequestBody RenomearDocumentoRequestDTO request
    ) {
        if (request.getNomeFicheiro() == null || request.getNomeFicheiro().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        DocumentoContabilistico documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        documento.setNomeFicheiro(request.getNomeFicheiro().trim());
        documentoRepository.save(documento);

        return ResponseEntity.ok(enriquecimentoService.converter(documento));
    }

    /**
     * Corrige o tipo/número/valor da Sugestao mais recente ligada ao
     * documento — para quando a classificação automática (regras/IA)
     * ficou errada. Não mexe em nenhum Lancamento já criado a partir desta
     * Sugestao (se a análise já tiver sido aprovada): isto corrige o
     * rótulo mostrado no arquivo, não reabre partidas dobradas já
     * lançadas — essas continuam a editar-se em Lancamentos (PUT
     * /api/lancamentos/{id}).
     */
    // RN010: escrita fica fora do Auditor, mesma regra do upload.
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CONTABILISTA')")
    @PatchMapping("/{id}/classificacao")
    @Transactional
    public ResponseEntity<DocumentoResponseDTO> corrigirClassificacao(
            @PathVariable Long id,
            @RequestBody CorrigirClassificacaoRequestDTO request
    ) {
        DocumentoContabilistico documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        Sugestao maisRecente = sugestaoRepository.findTopByDocumentoIdOrderByDataCriacaoDesc(id)
                .orElseThrow(() -> new RuntimeException(
                        "Este documento ainda não foi analisado — não há classificação para corrigir"));

        if (request.getTipoDocumento() != null) {
            maisRecente.setTipoDocumento(request.getTipoDocumento());
        }
        if (request.getNumeroDocumento() != null) {
            maisRecente.setNumeroDocumento(request.getNumeroDocumento());
        }
        if (request.getValor() != null) {
            maisRecente.setValor(request.getValor());
        }
        sugestaoRepository.save(maisRecente);

        return ResponseEntity.ok(enriquecimentoService.converter(documento));
    }

    /**
     * Serve o ficheiro original com o Content-Type real, para o browser
     * conseguir abrir/pré-visualizar (PDF, imagem) em vez de só devolver JSON
     * com o conteúdo em base64 (ver GET /documentos/{id}).
     */
    @GetMapping("/{id}/download")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return documentoRepository.findById(id)
                .map(doc -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(
                                doc.getTipoConteudo() != null ? doc.getTipoConteudo() : "application/octet-stream"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + doc.getNomeFicheiro() + "\"")
                        .body(doc.getConteudo()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * T2 — arquivo por entidade: exporta todos os documentos num .zip com uma
     * pasta por entidade (cliente/fornecedor), cada uma com os ficheiros
     * originais e um manifesto.csv ligando documento → sugestão → lançamento
     * → contas. Serve para justificar a origem de um lançamento numa
     * fiscalização. Filtro de datas opcional (sobre a data de upload).
     */
    @GetMapping("/export-zip")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportarZip(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) throws IOException {

        List<DocumentoContabilistico> documentos = documentoRepository.findAll().stream()
                .filter(d -> dentroDoIntervalo(d, inicio, fim))
                .toList();

        byte[] zip = gerarZip(documentos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documentos.zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    private boolean dentroDoIntervalo(DocumentoContabilistico documento, LocalDate inicio, LocalDate fim) {
        if (documento.getDataUpload() == null) {
            return true;
        }
        LocalDate data = documento.getDataUpload().toLocalDate();
        if (inicio != null && data.isBefore(inicio)) {
            return false;
        }
        return fim == null || !data.isAfter(fim);
    }

    private byte[] gerarZip(List<DocumentoContabilistico> documentos) throws IOException {
        List<Long> entidadeIds = documentos.stream()
                .map(DocumentoContabilistico::getEntidadeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Entidade> entidadesPorId = entidadeRepository.findAllById(entidadeIds).stream()
                .collect(Collectors.toMap(Entidade::getId, e -> e));

        Map<String, List<DocumentoContabilistico>> porPasta = documentos.stream()
                .collect(Collectors.groupingBy(d -> nomePasta(d, entidadesPorId)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, List<DocumentoContabilistico>> pasta : porPasta.entrySet()) {
                escreverPasta(zip, pasta.getKey(), pasta.getValue());
            }
        }

        return out.toByteArray();
    }

    private String nomePasta(DocumentoContabilistico documento, Map<Long, Entidade> entidadesPorId) {
        if (documento.getEntidadeId() == null) {
            return "Documentos nao analisados";
        }
        Entidade entidade = entidadesPorId.get(documento.getEntidadeId());
        if (entidade == null || entidade.getTipo() == null) {
            return "Sem identificacao";
        }
        return switch (entidade.getTipo()) {
            case CLIENTE -> "Cliente - " + sanitizarNomePasta(entidade.getNome());
            case FORNECEDOR -> "Fornecedor - " + sanitizarNomePasta(entidade.getNome());
            case DESCONHECIDO -> "Sem identificacao";
        };
    }

    private String sanitizarNomePasta(String nome) {
        if (nome == null || nome.isBlank()) {
            return "Desconhecido";
        }
        return nome.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void escreverPasta(ZipOutputStream zip, String pasta, List<DocumentoContabilistico> documentos) throws IOException {
        StringBuilder manifesto = new StringBuilder(
                "documentoId,ficheiro,dataUpload,sugestaoId,lancamentoId,estadoLancamento,conta,debito,credito\n");

        for (DocumentoContabilistico documento : documentos) {
            zip.putNextEntry(new ZipEntry(pasta + "/" + documento.getId() + "_" + documento.getNomeFicheiro()));
            if (documento.getConteudo() != null) {
                zip.write(documento.getConteudo());
            }
            zip.closeEntry();

            adicionarLinhasManifesto(manifesto, documento);
        }

        zip.putNextEntry(new ZipEntry(pasta + "/manifesto.csv"));
        zip.write(manifesto.toString().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void adicionarLinhasManifesto(StringBuilder manifesto, DocumentoContabilistico documento) {
        // Um documento pode ter sido analisado mais do que uma vez (nova
        // tentativa, reanálise) — o manifesto regista todas as sugestões,
        // não só a mais recente, para não esconder histórico numa auditoria.
        List<Sugestao> sugestoes = sugestaoRepository.findAllByDocumentoId(documento.getId());

        if (sugestoes.isEmpty()) {
            linhaManifesto(manifesto, documento, null, null, null, null, null, null);
            return;
        }

        for (Sugestao sugestao : sugestoes) {
            Optional<Lancamento> lancamentoOpt = sugestao.getLancamentoId() != null
                    ? lancamentoRepository.findById(sugestao.getLancamentoId())
                    : Optional.empty();

            if (lancamentoOpt.isEmpty() || lancamentoOpt.get().getLinhas().isEmpty()) {
                linhaManifesto(manifesto, documento, sugestao.getId(), sugestao.getLancamentoId(), null, null, null, null);
                continue;
            }

            Lancamento lancamento = lancamentoOpt.get();
            for (LinhaLancamento linha : lancamento.getLinhas()) {
                linhaManifesto(
                        manifesto, documento, sugestao.getId(), lancamento.getId(),
                        lancamento.getEstado(), linha.getConta(), linha.getDebito(), linha.getCredito());
            }
        }
    }

    private void linhaManifesto(StringBuilder manifesto, DocumentoContabilistico documento,
                                 Long sugestaoId, Long lancamentoId, Object estadoLancamento,
                                 String conta, java.math.BigDecimal debito, java.math.BigDecimal credito) {
        manifesto.append(documento.getId()).append(',')
                .append(escaparCsv(documento.getNomeFicheiro())).append(',')
                .append(documento.getDataUpload()).append(',')
                .append(sugestaoId == null ? "" : sugestaoId).append(',')
                .append(lancamentoId == null ? "" : lancamentoId).append(',')
                .append(estadoLancamento == null ? "" : estadoLancamento).append(',')
                .append(escaparCsv(conta)).append(',')
                .append(debito == null ? "" : debito).append(',')
                .append(credito == null ? "" : credito)
                .append('\n');
    }

    private String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
