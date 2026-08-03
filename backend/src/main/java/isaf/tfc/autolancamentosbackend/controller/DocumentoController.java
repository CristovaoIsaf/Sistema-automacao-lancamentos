package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.DocumentoResponseDTO;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
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

    public DocumentoController(DocumentoRepository documentoRepository,
                                EntidadeRepository entidadeRepository,
                                SugestaoRepository sugestaoRepository,
                                LancamentoRepository lancamentoRepository) {
        this.documentoRepository = documentoRepository;
        this.entidadeRepository = entidadeRepository;
        this.sugestaoRepository = sugestaoRepository;
        this.lancamentoRepository = lancamentoRepository;
    }

    /**
     * Upload de um documento (PDF ou imagem). Guarda os bytes no banco.
     * O ficheiro fica pronto para ser analisado via POST /analises com o documentoId devolvido aqui.
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentoContabilistico> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setNomeFicheiro(file.getOriginalFilename());
        documento.setTipoConteudo(file.getContentType());
        documento.setConteudo(file.getBytes());
        documento.setUserId(user.getId());

        DocumentoContabilistico salvo = documentoRepository.save(documento);
        return ResponseEntity.ok(salvo);
    }

    /**
     * Lista os documentos carregados pelo utilizador autenticado (sem o
     * conteudo do ficheiro, que continua disponível em GET /documentos/{id}).
     * Inclui entidade e estado (Pendente/Analisado/Aprovado/Rejeitado) para a
     * página de Arquivo agrupar e filtrar os documentos — mesma lógica de
     * agrupamento por entidade e de rastreio Documento→Sugestao já usada em
     * gerarZip()/adicionarLinhasManifesto().
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<DocumentoResponseDTO>> listar(@AuthenticationPrincipal User user) {
        List<DocumentoContabilistico> documentos = documentoRepository.findByUserId(user.getId());

        Map<Long, Entidade> entidadesPorId = entidadeRepository.findAllById(
                documentos.stream()
                        .map(DocumentoContabilistico::getEntidadeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        ).stream().collect(Collectors.toMap(Entidade::getId, e -> e));

        List<DocumentoResponseDTO> resposta = documentos.stream()
                .map(doc -> {
                    Entidade entidade = doc.getEntidadeId() != null ? entidadesPorId.get(doc.getEntidadeId()) : null;
                    return new DocumentoResponseDTO(
                            doc.getId(),
                            doc.getNomeFicheiro(),
                            doc.getTipoConteudo(),
                            doc.getDataUpload(),
                            doc.getEntidadeId(),
                            entidade != null ? entidade.getNome() : null,
                            doc.getConteudo() != null ? doc.getConteudo().length : 0,
                            estadoDocumento(doc)
                    );
                })
                .toList();

        return ResponseEntity.ok(resposta);
    }

    private String estadoDocumento(DocumentoContabilistico documento) {
        List<Sugestao> sugestoes = sugestaoRepository.findAllByDocumentoId(documento.getId());
        if (sugestoes.isEmpty()) {
            return "Pendente";
        }

        Sugestao maisRecente = sugestoes.stream()
                .max(Comparator.comparing(Sugestao::getDataCriacao))
                .orElse(sugestoes.get(0));

        return switch (maisRecente.getEstado()) {
            case APROVADA -> "Aprovado";
            case REJEITADA -> "Rejeitado";
            case PENDENTE -> "Analisado";
        };
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<DocumentoContabilistico> buscar(@PathVariable Long id) {
        return documentoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
