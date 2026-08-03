package isaf.tfc.autolancamentosbackend.repository;

import isaf.tfc.autolancamentosbackend.model.Sugestao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SugestaoRepository extends JpaRepository<Sugestao, Long> {

    // Um documento pode ter sido analisado mais do que uma vez (nova
    // tentativa, reanálise) — por isso é uma lista, não um resultado único.
    List<Sugestao> findAllByDocumentoId(Long documentoId);
}
