package isaf.tfc.autolancamentosbackend.repository;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoRepository extends JpaRepository<DocumentoContabilistico, Long> {
    List<DocumentoContabilistico> findByUserId(Long userId);

    // Fase 10 do plano de 20 fases — dossiê da entidade (ver EntidadeController).
    List<DocumentoContabilistico> findByEntidadeId(Long entidadeId);

    Optional<DocumentoContabilistico> findByHashConteudo(String hashConteudo);
}