package isaf.tfc.autolancamentosbackend.repository;

import isaf.tfc.autolancamentosbackend.model.LancamentoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LancamentoHistoricoRepository extends JpaRepository<LancamentoHistorico, Long> {
    List<LancamentoHistorico> findByLancamentoIdOrderByAlteradoEmDesc(Long lancamentoId);
}
