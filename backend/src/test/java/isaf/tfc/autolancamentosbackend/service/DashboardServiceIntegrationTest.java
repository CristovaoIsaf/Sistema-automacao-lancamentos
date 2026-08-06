package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.DashboardResponseDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração real do bloco "Dashboard": liga a peça "Partidas
 * dobradas" (um Lancamento equilibrado real, como os que
 * AnaliseContabilService cria) à peça "DashboardService" — confirma que um
 * lançamento persistido de verdade aparece corretamente classificado no
 * gráfico mensal, sem mocks. @Transactional faz rollback no fim.
 */
@SpringBootTest
@Transactional
class DashboardServiceIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void obterDashboard_comLancamentoRealEquilibrado_apareceNoGraficoDoMesCorreto() {
        Lancamento lancamento = new Lancamento();
        lancamento.setData(LocalDate.now());
        lancamento.setDescricao("Venda real — integração dashboard");
        lancamento.setEstado(EstadoLancamento.VALIDADO);
        lancamento.setOrigem(OrigemLancamento.AUTOMATICO);

        LinhaLancamento debito = new LinhaLancamento();
        debito.setConta("31");
        debito.setDebito(new BigDecimal("50000.00"));
        debito.setLancamento(lancamento);
        lancamento.getLinhas().add(debito);

        LinhaLancamento credito = new LinhaLancamento();
        credito.setConta("61");
        credito.setCredito(new BigDecimal("50000.00"));
        credito.setLancamento(lancamento);
        lancamento.getLinhas().add(credito);

        lancamentoRepository.save(lancamento);

        entityManager.flush();
        entityManager.clear();

        DashboardResponseDTO resposta = dashboardService.obterDashboard(1, 5);

        assertThat(resposta.getGraficoMensal()).hasSize(1);
        assertThat(resposta.getGraficoMensal().get(0).getReceitas())
                .isGreaterThanOrEqualTo(new BigDecimal("50000.00"));
        assertThat(resposta.getKpis().getLancamentosAprovados()).isGreaterThanOrEqualTo(1);
    }
}
