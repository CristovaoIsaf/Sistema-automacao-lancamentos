package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Peça "Lançamento manual": LancamentoServiceImpl isolado (repositório
 * mockado). Cobre em particular a correção do NullPointerException em
 * validarEquilibrio — cada linha normalmente só tem débito OU crédito
 * (nunca os dois), que é exatamente o caso real de um multilançamento com
 * linha de IVA separada.
 */
class LancamentoServiceImplTest {

    private LancamentoRepository repository;
    private LancamentoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LancamentoRepository.class);
        service = new LancamentoServiceImpl(repository);
        when(repository.save(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });
    }

    @Test
    void criarLancamentoManual_multilancamentoComLinhasSoDebitoOuSoCredito_naoLancaNullPointer() {
        LancamentoRequestDTO request = new LancamentoRequestDTO(
                LocalDate.now(),
                "Venda com IVA 14%",
                List.of(
                        new LinhaLancamentoDTO("31", new BigDecimal("114000.00"), null, "Clientes"),
                        new LinhaLancamentoDTO("61", null, new BigDecimal("100000.00"), "Vendas"),
                        new LinhaLancamentoDTO("34.5.2", null, new BigDecimal("14000.00"), "IVA liquidado 14%")
                )
        );

        LancamentoResponseDTO resposta = service.criarLancamentoManual(request, 7L);

        assertThat(resposta.getLinhas()).hasSize(3);
        assertThat(resposta.getId()).isNotNull();
    }

    @Test
    void criarLancamentoManual_preservaDescricaoDaLinhaERegistaQuemCriou() {
        // Fase 7 do plano de 20 fases: antes desta fase, LinhaLancamento.descricao
        // ficava sempre null para lançamentos manuais (só a descrição do
        // Lancamento era gravada) e Lancamento.validadoPor nunca era
        // preenchido — nenhum registo de quem criou o lançamento.
        LancamentoRequestDTO request = new LancamentoRequestDTO(
                LocalDate.now(),
                "Pagamento de renda",
                List.of(
                        new LinhaLancamentoDTO("75.2.21", new BigDecimal("50000.00"), null, "Renda do escritório — Março"),
                        new LinhaLancamentoDTO("45", null, new BigDecimal("50000.00"), null)
                )
        );

        LancamentoResponseDTO resposta = service.criarLancamentoManual(request, 42L);

        assertThat(resposta.getLinhas().get(0).getDescricao()).isEqualTo("Renda do escritório — Março");
        // Linha sem descrição própria cai no fallback da descrição do lançamento.
        assertThat(resposta.getLinhas().get(1).getDescricao()).isEqualTo("Pagamento de renda");

        ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getValidadoPor()).isEqualTo(42L);
    }

    @Test
    void criarLancamentoManual_desequilibrado_lancaExcecaoComMensagemClara() {
        LancamentoRequestDTO request = new LancamentoRequestDTO(
                LocalDate.now(),
                "Lançamento errado",
                List.of(
                        new LinhaLancamentoDTO("31", new BigDecimal("100000.00"), null, null),
                        new LinhaLancamentoDTO("61", null, new BigDecimal("90000.00"), null)
                )
        );

        assertThatThrownBy(() -> service.criarLancamentoManual(request, 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não está equilibrado");
    }

    @Test
    void criarLancamentoManual_comTaxaDeIva7Porcento_ficaEquilibrado() {
        // Base 100.000,00 + IVA 7% (7.000,00) = 107.000,00
        LancamentoRequestDTO request = new LancamentoRequestDTO(
                LocalDate.now(),
                "Venda com IVA 7% (taxa reduzida)",
                List.of(
                        new LinhaLancamentoDTO("31", new BigDecimal("107000.00"), null, "Clientes"),
                        new LinhaLancamentoDTO("61", null, new BigDecimal("100000.00"), "Vendas"),
                        new LinhaLancamentoDTO("34.5.2", null, new BigDecimal("7000.00"), "IVA liquidado 7%")
                )
        );

        LancamentoResponseDTO resposta = service.criarLancamentoManual(request, 7L);

        assertThat(resposta.getLinhas()).hasSize(3);
    }
}
