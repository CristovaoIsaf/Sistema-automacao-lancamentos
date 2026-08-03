package isaf.tfc.autolancamentosbackend.repository;

import isaf.tfc.autolancamentosbackend.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
