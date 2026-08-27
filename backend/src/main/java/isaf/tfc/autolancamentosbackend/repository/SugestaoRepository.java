package isaf.tfc.autolancamentosbackend.repository;

import isaf.tfc.autolancamentosbackend.model.Sugestao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SugestaoRepository extends JpaRepository<Sugestao, Long> {

    // Um documento pode ter sido analisado mais do que uma vez (nova
    // tentativa, reanálise) — por isso é uma lista, não um resultado único.
    List<Sugestao> findAllByDocumentoId(Long documentoId);

    // Auditoria de performance — versão em lote de findAllByDocumentoId,
    // usada por DocumentoEnriquecimentoService.converterTodos para evitar
    // uma query por documento (N+1 confirmado: antes, listar N documentos
    // disparava N chamadas a findAllByDocumentoId, uma por documento).
    List<Sugestao> findByDocumentoIdIn(List<Long> documentoIds);

    // Mesma noção de "sugestão mais recente" que
    // DocumentoEnriquecimentoService.sugestaoMaisRecente() usa para a
    // listagem — reutilizada por PATCH /documentos/{id}/classificacao para
    // saber qual Sugestao corrigir.
    Optional<Sugestao> findTopByDocumentoIdOrderByDataCriacaoDesc(Long documentoId);

    // Fase 3 — Context Engine: histórico relevante de uma entidade (as
    // classificações mais recentes dos seus documentos), usado para
    // construir o "contexto" compacto de uma operação. Sub-query em vez de
    // relação JPA porque Documento/Sugestao não têm relação directa (ver
    // padrão de ids soltos já usado no resto do projecto).
    @Query("""
            SELECT s FROM Sugestao s
            WHERE s.documentoId IN (
                SELECT d.id FROM DocumentoContabilistico d WHERE d.entidadeId = :entidadeId
            )
            ORDER BY s.dataCriacao DESC
            """)
    List<Sugestao> findRecentesPorEntidade(@Param("entidadeId") Long entidadeId, Pageable pageable);
}
