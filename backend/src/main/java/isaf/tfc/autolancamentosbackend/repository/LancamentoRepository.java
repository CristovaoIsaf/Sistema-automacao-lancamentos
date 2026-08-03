package isaf.tfc.autolancamentosbackend.repository;

import isaf.tfc.autolancamentosbackend.model.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento,Long> {

    List<Lancamento> findByDataBetween(
            LocalDate inicio,
            LocalDate fim
    );
}
