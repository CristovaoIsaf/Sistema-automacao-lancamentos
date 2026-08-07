package isaf.tfc.autolancamentosbackend.repository;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoRepository extends JpaRepository<DocumentoContabilistico, Long> {
    List<DocumentoContabilistico> findByUserId(Long userId);

    Optional<DocumentoContabilistico> findByHashConteudo(String hashConteudo);
}