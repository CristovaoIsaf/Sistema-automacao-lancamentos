package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.DocumentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Fase 10 do plano de 20 fases ("dossiê da entidade"): único ponto que
 * converte DocumentoContabilistico → DocumentoResponseDTO, incluindo
 * entidade e o tipo/número/valor da Sugestao mais recente. Usado tanto por
 * DocumentoController.listar() (arquivo completo) como por
 * EntidadeController.dossie() (documentos de uma única entidade) — mesmo
 * mecanismo, para as duas vistas nunca mostrarem dados diferentes para o
 * mesmo documento.
 */
@Service
public class DocumentoEnriquecimentoService {

    private final EntidadeRepository entidadeRepository;
    private final SugestaoRepository sugestaoRepository;

    public DocumentoEnriquecimentoService(EntidadeRepository entidadeRepository, SugestaoRepository sugestaoRepository) {
        this.entidadeRepository = entidadeRepository;
        this.sugestaoRepository = sugestaoRepository;
    }

    public DocumentoResponseDTO converter(DocumentoContabilistico documento) {
        return converterTodos(List.of(documento)).get(0);
    }

    public List<DocumentoResponseDTO> converterTodos(List<DocumentoContabilistico> documentos) {
        Map<Long, Entidade> entidadesPorId = entidadeRepository.findAllById(
                documentos.stream()
                        .map(DocumentoContabilistico::getEntidadeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        ).stream().collect(Collectors.toMap(Entidade::getId, e -> e));

        return documentos.stream()
                .map(doc -> construir(doc, entidadesPorId))
                .toList();
    }

    private DocumentoResponseDTO construir(DocumentoContabilistico doc, Map<Long, Entidade> entidadesPorId) {
        Entidade entidade = doc.getEntidadeId() != null ? entidadesPorId.get(doc.getEntidadeId()) : null;
        Sugestao maisRecente = sugestaoMaisRecente(doc.getId());

        return new DocumentoResponseDTO(
                doc.getId(),
                doc.getNomeFicheiro(),
                doc.getTipoConteudo(),
                doc.getDataUpload(),
                doc.getEntidadeId(),
                entidade != null ? entidade.getNome() : null,
                entidade != null ? entidade.getNif() : null,
                doc.getConteudo() != null ? doc.getConteudo().length : 0,
                estadoDaSugestao(maisRecente),
                maisRecente != null ? maisRecente.getTipoDocumento() : null,
                maisRecente != null ? maisRecente.getNumeroDocumento() : null,
                maisRecente != null ? maisRecente.getValor() : null
        );
    }

    private Sugestao sugestaoMaisRecente(Long documentoId) {
        return sugestaoRepository.findAllByDocumentoId(documentoId).stream()
                .max(Comparator.comparing(Sugestao::getDataCriacao))
                .orElse(null);
    }

    private String estadoDaSugestao(Sugestao maisRecente) {
        if (maisRecente == null) {
            return "Pendente";
        }
        return switch (maisRecente.getEstado()) {
            case APROVADA -> "Aprovado";
            case REJEITADA -> "Rejeitado";
            case PENDENTE -> "Analisado";
        };
    }
}
