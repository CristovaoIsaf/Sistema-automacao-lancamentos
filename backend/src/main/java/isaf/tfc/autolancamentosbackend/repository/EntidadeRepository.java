package isaf.tfc.autolancamentosbackend.repository;

import isaf.tfc.autolancamentosbackend.model.Entidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntidadeRepository extends JpaRepository<Entidade, Long> {

    Optional<Entidade> findByNif(String nif);
}
