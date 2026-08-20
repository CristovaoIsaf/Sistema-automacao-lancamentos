package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.model.LancamentoHistorico;
import isaf.tfc.autolancamentosbackend.repository.LancamentoHistoricoRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Peça "Lançamento manual": LancamentoServiceImpl isolado (repositório e
 * LancamentoEnriquecimentoService mockados — este teste cobre a construção
 * do Lancamento em si, não o enriquecimento com entidade/utilizador, que
 * tem cobertura própria em LancamentoEnriquecimentoServiceTest). Cobre em
 * particular a correção do NullPointerException em validarEquilibrio —
 * cada linha normalmente só tem débito OU crédito (nunca os dois), que é
 * exatamente o caso real de um multilançamento com linha de IVA separada.
 *
 * Auditoria C01/C03: cobre também a separação criação/aprovação de
 * lançamentos manuais e o fluxo de pedido/aprovação/rejeição de anulação.
 */
class LancamentoServiceImplTest {

    private LancamentoRepository repository;
    private PlanoContasClient planoContasClient;
    private LancamentoHistoricoRepository historicoRepository;
    private UserRepository userRepository;
    private LancamentoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LancamentoRepository.class);
        LancamentoEnriquecimentoService enriquecimentoService = Mockito.mock(LancamentoEnriquecimentoService.class);
        planoContasClient = Mockito.mock(PlanoContasClient.class);
        // Fail-open por omissão (ver PartidasDobradas.validarContasExistem):
        // lista vazia = plano de contas ainda não disponível, validação
        // salta-se — os testes desta classe continuam a usar códigos de
        // conta fictícios ("31", "61", ...) sem precisar de os registar
        // aqui um por um. Os testes dedicados a C14 abaixo sobrepõem isto.
        when(planoContasClient.listar()).thenReturn(List.of());
        historicoRepository = Mockito.mock(LancamentoHistoricoRepository.class);
        when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        userRepository = Mockito.mock(UserRepository.class);
        when(userRepository.findAllById(any())).thenReturn(List.of());
        ObjectMapper objectMapper = new ObjectMapper();
        service = new LancamentoServiceImpl(
                repository, enriquecimentoService, planoContasClient, historicoRepository, userRepository, objectMapper);

        when(repository.save(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(1L);
            }
            return l;
        });
        // Simula a conversão real (sem o enriquecimento por entidade/
        // utilizador, irrelevante para os testes desta classe).
        when(enriquecimentoService.converter(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            LancamentoResponseDTO dto = new LancamentoResponseDTO();
            dto.setId(l.getId());
            dto.setData(l.getData());
            dto.setDescricao(l.getDescricao());
            dto.setEstado(l.getEstado());
            dto.setOrigem(l.getOrigem());
            dto.setEditadoManualmente(l.getEditadoManualmente());
            dto.setCriadoPor(l.getCriadoPor());
            dto.setValidadoPor(l.getValidadoPor());
            dto.setEstornoDeId(l.getEstornoDeId());
            dto.setLinhas(l.getLinhas().stream()
                    .map(linha -> new LinhaLancamentoDTO(linha.getConta(), linha.getDebito(), linha.getCredito(), linha.getDescricao()))
                    .toList());
            return dto;
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
        assertThat(captor.getValue().getCriadoPor()).isEqualTo(42L);
    }

    // Auditoria C01: até esta correção, criarLancamentoManual gravava
    // criadoPor diretamente em validadoPor e o estado nascia já VALIDADO —
    // a mesma pessoa criava e aprovava na mesma chamada. Agora nasce
    // PENDENTE, sem validadoPor nenhum, à espera de aprovar().
    @Test
    void criarLancamentoManual_nasceProvisorioSemAutoAprovacao() {
        LancamentoRequestDTO request = new LancamentoRequestDTO(
                LocalDate.now(),
                "Compra de material de escritório",
                List.of(
                        new LinhaLancamentoDTO("62", new BigDecimal("30000.00"), null, null),
                        new LinhaLancamentoDTO("45", null, new BigDecimal("30000.00"), null)
                )
        );

        LancamentoResponseDTO resposta = service.criarLancamentoManual(request, 7L);

        assertThat(resposta.getEstado()).isEqualTo(EstadoLancamento.PENDENTE);
        assertThat(resposta.getValidadoPor()).isNull();
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

    // --- validação de conta contra o plano de contas — Auditoria C14 ---

    @Test
    void criarLancamentoManual_contaInexistenteNoPlanoDeContas_lancaExcecao() {
        when(planoContasClient.listar()).thenReturn(List.of(
                new ContaDTO("31", "Clientes"), new ContaDTO("61", "Vendas")));

        LancamentoRequestDTO request = new LancamentoRequestDTO(
                LocalDate.now(),
                "Lançamento com conta inventada",
                List.of(
                        new LinhaLancamentoDTO("99.99", new BigDecimal("1000.00"), null, null),
                        new LinhaLancamentoDTO("61", null, new BigDecimal("1000.00"), null)
                )
        );

        assertThatThrownBy(() -> service.criarLancamentoManual(request, 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99.99");
    }

    @Test
    void criarLancamentoManual_todasAsContasExistemNoPlano_naoLancaExcecao() {
        when(planoContasClient.listar()).thenReturn(List.of(
                new ContaDTO("31", "Clientes"), new ContaDTO("61", "Vendas")));

        LancamentoRequestDTO request = new LancamentoRequestDTO(
                LocalDate.now(),
                "Venda à vista",
                List.of(
                        new LinhaLancamentoDTO("31", new BigDecimal("1000.00"), null, null),
                        new LinhaLancamentoDTO("61", null, new BigDecimal("1000.00"), null)
                )
        );

        LancamentoResponseDTO resposta = service.criarLancamentoManual(request, 7L);

        assertThat(resposta.getLinhas()).hasSize(2);
    }

    @Test
    void criarLancamentoManual_planoDeContasIndisponivel_naoBloqueiaOLancamento() {
        // FastAPI em baixo (ver PlanoContasClient.tentarCarregar) — lista
        // vazia não pode significar "nenhuma conta é válida".
        when(planoContasClient.listar()).thenReturn(List.of());

        LancamentoRequestDTO request = new LancamentoRequestDTO(
                LocalDate.now(),
                "Venda à vista",
                List.of(
                        new LinhaLancamentoDTO("31", new BigDecimal("1000.00"), null, null),
                        new LinhaLancamentoDTO("61", null, new BigDecimal("1000.00"), null)
                )
        );

        LancamentoResponseDTO resposta = service.criarLancamentoManual(request, 7L);

        assertThat(resposta.getLinhas()).hasSize(2);
    }

    // --- aprovar() — Auditoria C01 -----------------------------------

    private Lancamento lancamentoPendente(Long id, Long criadoPor) {
        Lancamento l = new Lancamento();
        l.setId(id);
        l.setEstado(EstadoLancamento.PENDENTE);
        l.setOrigem(OrigemLancamento.MANUAL);
        l.setCriadoPor(criadoPor);
        l.setData(LocalDate.now());
        l.setDescricao("Lançamento " + id);
        LinhaLancamento linha = new LinhaLancamento();
        linha.setConta("62");
        linha.setDebito(new BigDecimal("1000"));
        linha.setLancamento(l);
        l.getLinhas().add(linha);
        return l;
    }

    @Test
    void aprovar_porQuemCriou_lancaExcecaoENaoAlteraEstado() {
        when(repository.findById(1L)).thenReturn(Optional.of(lancamentoPendente(1L, 7L)));

        assertThatThrownBy(() -> service.aprovar(1L, 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Não pode aprovar um lançamento que criou");

        Mockito.verify(repository, Mockito.never()).save(any());
    }

    @Test
    void aprovar_porOutroContabilista_passaAValidadoEGravaQuemAprovou() {
        when(repository.findById(1L)).thenReturn(Optional.of(lancamentoPendente(1L, 7L)));

        LancamentoResponseDTO resposta = service.aprovar(1L, 42L);

        assertThat(resposta.getEstado()).isEqualTo(EstadoLancamento.VALIDADO);
        assertThat(resposta.getValidadoPor()).isEqualTo(42L);
    }

    @Test
    void aprovar_lancamentoJaValidado_lancaExcecao() {
        Lancamento validado = lancamentoPendente(1L, 7L);
        validado.setEstado(EstadoLancamento.VALIDADO);
        when(repository.findById(1L)).thenReturn(Optional.of(validado));

        assertThatThrownBy(() -> service.aprovar(1L, 42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("pendente");
    }

    // --- solicitarCancelamento/aprovarCancelamento/rejeitarCancelamento
    // --- Auditoria C03 -------------------------------------------------

    private Lancamento lancamentoValidado(Long id, Long validadoPor) {
        Lancamento l = new Lancamento();
        l.setId(id);
        l.setEstado(EstadoLancamento.VALIDADO);
        l.setOrigem(OrigemLancamento.MANUAL);
        l.setValidadoPor(validadoPor);
        l.setData(LocalDate.now());
        l.setDescricao("Lançamento " + id);
        LinhaLancamento debito = new LinhaLancamento();
        debito.setConta("62");
        debito.setDebito(new BigDecimal("1000"));
        debito.setLancamento(l);
        LinhaLancamento credito = new LinhaLancamento();
        credito.setConta("45");
        credito.setCredito(new BigDecimal("1000"));
        credito.setLancamento(l);
        l.getLinhas().add(debito);
        l.getLinhas().add(credito);
        return l;
    }

    @Test
    void solicitarCancelamento_semMotivo_lancaExcecao() {
        assertThatThrownBy(() -> service.solicitarCancelamento(1L, "  ", 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("motivo");

        Mockito.verify(repository, Mockito.never()).findById(any());
    }

    @Test
    void solicitarCancelamento_lancamentoValidado_passaACancelamentoPendente() {
        when(repository.findById(1L)).thenReturn(Optional.of(lancamentoValidado(1L, 42L)));

        LancamentoResponseDTO resposta = service.solicitarCancelamento(1L, "Lançamento duplicado", 7L);

        assertThat(resposta.getEstado()).isEqualTo(EstadoLancamento.CANCELAMENTO_PENDENTE);
    }

    @Test
    void solicitarCancelamento_lancamentoAindaPendente_lancaExcecao() {
        when(repository.findById(1L)).thenReturn(Optional.of(lancamentoPendente(1L, 7L)));

        assertThatThrownBy(() -> service.solicitarCancelamento(1L, "Motivo qualquer", 42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("validado");
    }

    @Test
    void aprovarCancelamento_porQuemPediu_lancaExcecao() {
        Lancamento pedido = lancamentoValidado(1L, 42L);
        pedido.setEstado(EstadoLancamento.CANCELAMENTO_PENDENTE);
        pedido.setCancelamentoSolicitadoPor(7L);
        when(repository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.aprovarCancelamento(1L, 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Não pode aprovar a anulação que você próprio pediu");
    }

    @Test
    void aprovarCancelamento_porOutroContabilista_canceleOriginalEGeraEstornoComLinhasInvertidas() {
        Lancamento pedido = lancamentoValidado(1L, 42L);
        pedido.setEstado(EstadoLancamento.CANCELAMENTO_PENDENTE);
        pedido.setCancelamentoSolicitadoPor(7L);
        pedido.setMotivoCancelamento("Conta errada");
        when(repository.findById(1L)).thenReturn(Optional.of(pedido));

        LancamentoResponseDTO estorno = service.aprovarCancelamento(1L, 99L);

        // O que a resposta devolve é o ESTORNO (novo lançamento), não o original.
        assertThat(estorno.getEstado()).isEqualTo(EstadoLancamento.VALIDADO);
        assertThat(estorno.getValidadoPor()).isEqualTo(99L);
        assertThat(estorno.getEstornoDeId()).isEqualTo(1L);
        assertThat(estorno.getLinhas()).hasSize(2);
        // Linhas invertidas: onde o original tinha débito, o estorno tem crédito.
        assertThat(estorno.getLinhas())
                .anySatisfy(linha -> {
                    if (linha.getConta().equals("62")) {
                        assertThat(linha.getCredito()).isEqualByComparingTo("1000");
                        assertThat(linha.getDebito()).isNull();
                    }
                });

        // O original foi mesmo marcado como CANCELADO (2ª chamada a save()).
        ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
        Mockito.verify(repository, Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getEstado()).isEqualTo(EstadoLancamento.CANCELADO);
    }

    @Test
    void rejeitarCancelamento_porQuemPediu_lancaExcecao() {
        Lancamento pedido = lancamentoValidado(1L, 42L);
        pedido.setEstado(EstadoLancamento.CANCELAMENTO_PENDENTE);
        pedido.setCancelamentoSolicitadoPor(7L);
        when(repository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.rejeitarCancelamento(1L, 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Não pode rejeitar a anulação que você próprio pediu");
    }

    @Test
    void rejeitarCancelamento_porOutroContabilista_voltaAValidado() {
        Lancamento pedido = lancamentoValidado(1L, 42L);
        pedido.setEstado(EstadoLancamento.CANCELAMENTO_PENDENTE);
        pedido.setCancelamentoSolicitadoPor(7L);
        when(repository.findById(1L)).thenReturn(Optional.of(pedido));

        LancamentoResponseDTO resposta = service.rejeitarCancelamento(1L, 99L);

        assertThat(resposta.getEstado()).isEqualTo(EstadoLancamento.VALIDADO);
    }

    // --- atualizar()/listarHistorico() — Auditoria C04 -------------------

    @Test
    void atualizar_gravaSnapshotDoEstadoAnteriorAntesDeSobrescrever() {
        Lancamento existente = lancamentoValidado(1L, 42L);
        existente.setDescricao("Descrição original");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        LancamentoRequestDTO pedido = new LancamentoRequestDTO(
                LocalDate.now(),
                "Descrição corrigida",
                List.of(
                        new LinhaLancamentoDTO("62", new BigDecimal("2000.00"), null, null),
                        new LinhaLancamentoDTO("45", null, new BigDecimal("2000.00"), null)
                )
        );

        service.atualizar(1L, pedido, 99L);

        ArgumentCaptor<LancamentoHistorico> captor = ArgumentCaptor.forClass(LancamentoHistorico.class);
        Mockito.verify(historicoRepository).save(captor.capture());
        LancamentoHistorico snapshot = captor.getValue();
        assertThat(snapshot.getLancamentoId()).isEqualTo(1L);
        assertThat(snapshot.getDescricaoAnterior()).isEqualTo("Descrição original");
        // O snapshot é gravado ANTES da substituição — tem de conter os
        // valores de "62"/"45" a 1000, não os novos 2000.
        assertThat(snapshot.getLinhasAnterioresJson()).contains("1000").doesNotContain("2000");
    }

    @Test
    void listarHistorico_devolveLinhasAnterioresDesserializadas() {
        LancamentoHistorico versaoAntiga = new LancamentoHistorico();
        versaoAntiga.setId(5L);
        versaoAntiga.setLancamentoId(1L);
        versaoAntiga.setDescricaoAnterior("Versão antiga");
        versaoAntiga.setLinhasAnterioresJson(
                "[{\"conta\":\"62\",\"debito\":1000.00,\"credito\":null,\"descricao\":null}]");
        versaoAntiga.setAlteradoPor(42L);
        when(historicoRepository.findByLancamentoIdOrderByAlteradoEmDesc(1L)).thenReturn(List.of(versaoAntiga));

        List<isaf.tfc.autolancamentosbackend.dto.LancamentoHistoricoDTO> historico = service.listarHistorico(1L);

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getDescricaoAnterior()).isEqualTo("Versão antiga");
        assertThat(historico.get(0).getLinhasAnteriores()).hasSize(1);
        assertThat(historico.get(0).getLinhasAnteriores().get(0).getConta()).isEqualTo("62");
    }
}
