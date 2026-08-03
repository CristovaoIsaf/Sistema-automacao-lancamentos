package isaf.tfc.autolancamentosbackend.repository;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<DocumentoContabilistico, Long> {
    List<DocumentoContabilistico> findByUserId(Long userId);
}