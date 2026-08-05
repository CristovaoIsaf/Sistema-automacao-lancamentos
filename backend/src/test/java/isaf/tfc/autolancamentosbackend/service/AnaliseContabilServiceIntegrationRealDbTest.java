package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.EstadoSugestao;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Peça 3 do bloco "Partidas dobradas / IVA": integração real, ligando a
 * peça "Documento" (DocumentoRepositoryTest) à peça "aprovarSugestao"
 * (AnaliseContabilServiceTest) — desta vez contra a base de dados
 * configurada de verdade, sem mocks, confirmando que um Lancamento
 * equilibrado fica mesmo persistido a partir de uma Sugestao real ligada a
 * um Documento real. @Transactional faz rollback no fim.
 */
@SpringBootTest
@Transactional
class AnaliseContabilServiceIntegrationRealDbTest {

    @Autowired
    private AnaliseContabilService service;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private SugestaoRepository sugestaoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void aprovarSugestao_ligadaAUmDocumentoReal_persisteLancamentoEquilibrado() {
        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setNomeFicheiro("fatura-partidas-dobradas.png");
        documento.setTipoConteudo("image/png");
        documento.setConteudo(new byte[]{1, 2, 3});
        documento.setUserId(997L);
        documento = documentoRepository.save(documento);

        Sugestao sugestao = new Sugestao();
        sugestao.setDocumentoId(documento.getId());
        sugestao.setDescricao("Combustivel para veiculos da empresa");
        sugestao.setEstado(EstadoSugestao.PENDENTE);
        sugestao.setLinhasJson("""
                [
                  {"conta":"31","nome":"Clientes","debito":"50000.00","credito":null},
                  {"conta":"61","nome":"Vendas","debito":null,"credito":"43859.65"},
                  {"conta":"34.5.2","nome":"IVA liquidado","debito":null,"credito":"6140.35"}
                ]
                """);
        sugestao = sugestaoRepository.save(sugestao);

        entityManager.flush();
        entityManager.clear();

        LancamentoResponseDTO resposta = service.aprovarSugestao(sugestao.getId(), 4L);

        assertThat(resposta.getEstado()).isEqualTo(EstadoLancamento.VALIDADO);
        assertThat(resposta.getLinhas()).hasSize(3);

        BigDecimal totalDebito = resposta.getLinhas().stream()
                .map(l -> l.getDebito() != null ? l.getDebito() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredito = resposta.getLinhas().stream()
                .map(l -> l.getCredito() != null ? l.getCredito() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebito).isEqualByComparingTo(totalCredito);

        // Confirma que a Sugestao ficou mesmo marcada como aprovada na BD,
        // não só no objeto em memória devolvido pelo serviço.
        Sugestao sugestaoRecarregada = sugestaoRepository.findById(sugestao.getId()).orElseThrow();
        assertThat(sugestaoRecarregada.getEstado()).isEqualTo(EstadoSugestao.APROVADA);
        assertThat(sugestaoRecarregada.getLancamentoId()).isEqualTo(resposta.getId());
    }
}
