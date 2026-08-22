package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.AtividadeUtilizadorDTO;
import isaf.tfc.autolancamentosbackend.dto.LogAuditoriaDTO;
import isaf.tfc.autolancamentosbackend.dto.PaginaAuditoriaDTO;
import isaf.tfc.autolancamentosbackend.model.AuditLog;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.AuditLogRepository;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Fase 15 do plano de 20 fases — "consultar auditoria" / "acompanhar
 * contabilistas": AuditoriaService isolado (repositórios mockados).
 */
class AuditoriaServiceTest {

    private DocumentoRepository documentoRepository;
    private LancamentoRepository lancamentoRepository;
    private UserRepository userRepository;
    private AuditLogRepository auditLogRepository;
    private AuditoriaService service;

    private final User contabilista = new User(1L, "Ana Costa", "ana@exemplo.com", "5000000001", "ATIVO", "hash", Role.CONTABILISTA, null, null, false, null);

    @BeforeEach
    void setUp() {
        documentoRepository = Mockito.mock(DocumentoRepository.class);
        lancamentoRepository = Mockito.mock(LancamentoRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        when(userRepository.findAll()).thenReturn(List.of(contabilista));
        when(auditLogRepository.findAll()).thenReturn(List.of());
        service = new AuditoriaService(documentoRepository, lancamentoRepository, userRepository, auditLogRepository);
    }

    @Test
    void listarLogs_incluiEventoDeUploadDeDocumento() {
        when(documentoRepository.findAll()).thenReturn(List.of(documento(1L, contabilista.getId(), "fatura.png", LocalDateTime.of(2026, 8, 1, 10, 0))));
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        PaginaAuditoriaDTO pagina = service.listarLogs(null, null);

        assertThat(pagina.getItens()).hasSize(1);
        LogAuditoriaDTO log = pagina.getItens().get(0);
        assertThat(log.getUtilizador()).isEqualTo("Ana Costa");
        assertThat(log.getPerfil()).isEqualTo("CONTABILISTA");
        assertThat(log.getAcao()).isEqualTo("Importou documento");
        assertThat(log.getEntidade()).isEqualTo("fatura.png");
    }

    @Test
    void listarLogs_distingueLancamentoManualDeAprovacaoDeIA() {
        when(documentoRepository.findAll()).thenReturn(List.of());
        Lancamento manual = lancamento(1L, OrigemLancamento.MANUAL, contabilista.getId(), LocalDateTime.of(2026, 8, 1, 9, 0));
        Lancamento automatico = lancamento(2L, OrigemLancamento.AUTOMATICO, contabilista.getId(), LocalDateTime.of(2026, 8, 1, 11, 0));
        when(lancamentoRepository.findAll()).thenReturn(List.of(manual, automatico));

        PaginaAuditoriaDTO pagina = service.listarLogs(null, null);

        assertThat(pagina.getItens()).extracting(LogAuditoriaDTO::getAcao)
                .containsExactlyInAnyOrder("Registou lançamento manual", "Aprovou sugestão da IA");
    }

    @Test
    void listarLogs_ordenaPorDataDecrescente() {
        when(documentoRepository.findAll()).thenReturn(List.of(
                documento(1L, contabilista.getId(), "antigo.png", LocalDateTime.of(2026, 1, 1, 0, 0)),
                documento(2L, contabilista.getId(), "recente.png", LocalDateTime.of(2026, 8, 1, 0, 0))
        ));
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        PaginaAuditoriaDTO pagina = service.listarLogs(null, null);

        assertThat(pagina.getItens()).extracting(LogAuditoriaDTO::getEntidade)
                .containsExactly("recente.png", "antigo.png");
    }

    @Test
    void listarLogs_semCriadoEmUsaDataContabilisticaComoAproximacao() {
        // Fase 15 — lançamentos anteriores a esta fase não têm criadoEm
        // (aditivo); o evento continua incluído na auditoria, só com uma
        // data menos precisa (ver AuditoriaService.converterLancamento).
        when(documentoRepository.findAll()).thenReturn(List.of());
        Lancamento semCriadoEm = new Lancamento();
        semCriadoEm.setId(9L);
        semCriadoEm.setOrigem(OrigemLancamento.MANUAL);
        semCriadoEm.setValidadoPor(contabilista.getId());
        semCriadoEm.setData(LocalDate.of(2026, 3, 1));
        semCriadoEm.setDescricao("Lançamento antigo");
        semCriadoEm.setEstado(EstadoLancamento.VALIDADO);
        when(lancamentoRepository.findAll()).thenReturn(List.of(semCriadoEm));

        PaginaAuditoriaDTO pagina = service.listarLogs(null, null);

        assertThat(pagina.getItens()).hasSize(1);
        assertThat(pagina.getItens().get(0).getDataHora()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
    }

    @Test
    void listarLogs_paginaCorretamente() {
        when(documentoRepository.findAll()).thenReturn(List.of(
                documento(1L, contabilista.getId(), "a.png", LocalDateTime.of(2026, 1, 1, 0, 0)),
                documento(2L, contabilista.getId(), "b.png", LocalDateTime.of(2026, 2, 1, 0, 0)),
                documento(3L, contabilista.getId(), "c.png", LocalDateTime.of(2026, 3, 1, 0, 0))
        ));
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        PaginaAuditoriaDTO primeiraPagina = service.listarLogs(0, 2);

        assertThat(primeiraPagina.getItens()).hasSize(2);
        assertThat(primeiraPagina.getTotal()).isEqualTo(3);
    }

    @Test
    void listarLogs_lancamentoEditadoGeraEventoDistintoDeCriacao() {
        // Fase 16 — "alterações": editar não deve fingir ser criação; deve
        // aparecer como um segundo evento, atribuído a quem editou (que
        // pode ser diferente de quem criou/aprovou).
        User administrador = new User(2L, "Beatriz Neto", "beatriz@exemplo.com", "5000000002", "ATIVO", "hash", Role.ADMINISTRADOR, null, null, false, null);
        when(userRepository.findAll()).thenReturn(List.of(contabilista, administrador));
        when(documentoRepository.findAll()).thenReturn(List.of());

        Lancamento editado = lancamento(1L, OrigemLancamento.MANUAL, contabilista.getId(), LocalDateTime.of(2026, 8, 1, 9, 0));
        editado.setAtualizadoEm(LocalDateTime.of(2026, 8, 2, 15, 30));
        editado.setAlteradoPor(administrador.getId());
        when(lancamentoRepository.findAll()).thenReturn(List.of(editado));

        PaginaAuditoriaDTO pagina = service.listarLogs(null, null);

        assertThat(pagina.getItens()).hasSize(2);
        LogAuditoriaDTO eventoEdicao = pagina.getItens().stream()
                .filter(l -> l.getAcao().equals("Editou lançamento"))
                .findFirst().orElseThrow();
        assertThat(eventoEdicao.getUtilizador()).isEqualTo("Beatriz Neto");
        assertThat(eventoEdicao.getDataHora()).isEqualTo(LocalDateTime.of(2026, 8, 2, 15, 30));
    }

    @Test
    void listarLogs_lancamentoCanceladoGeraEventoDeCancelamento() {
        when(documentoRepository.findAll()).thenReturn(List.of());

        Lancamento cancelado = lancamento(1L, OrigemLancamento.MANUAL, contabilista.getId(), LocalDateTime.of(2026, 8, 1, 9, 0));
        cancelado.setEstado(EstadoLancamento.CANCELADO);
        cancelado.setAtualizadoEm(LocalDateTime.of(2026, 8, 3, 10, 0));
        cancelado.setAlteradoPor(contabilista.getId());
        when(lancamentoRepository.findAll()).thenReturn(List.of(cancelado));

        PaginaAuditoriaDTO pagina = service.listarLogs(null, null);

        assertThat(pagina.getItens()).extracting(LogAuditoriaDTO::getAcao)
                .contains("Cancelou lançamento");
    }

    @Test
    void listarLogs_lancamentoNuncaAlteradoNaoGeraEventoDeAlteracao() {
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamento(1L, OrigemLancamento.MANUAL, contabilista.getId(), LocalDateTime.of(2026, 8, 1, 9, 0))
        ));

        PaginaAuditoriaDTO pagina = service.listarLogs(null, null);

        assertThat(pagina.getItens()).hasSize(1);
        assertThat(pagina.getItens()).extracting(LogAuditoriaDTO::getAcao)
                .doesNotContain("Editou lançamento", "Cancelou lançamento");
    }

    @Test
    void resumoPorUtilizador_contaAcoesPorUtilizador() {
        when(documentoRepository.findAll()).thenReturn(List.of(
                documento(1L, contabilista.getId(), "a.png", LocalDateTime.of(2026, 1, 1, 0, 0)),
                documento(2L, contabilista.getId(), "b.png", LocalDateTime.of(2026, 2, 1, 0, 0))
        ));
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamento(1L, OrigemLancamento.MANUAL, contabilista.getId(), LocalDateTime.of(2026, 3, 1, 0, 0))
        ));

        List<AtividadeUtilizadorDTO> resumo = service.resumoPorUtilizador();

        assertThat(resumo).hasSize(1);
        assertThat(resumo.get(0).getUtilizador()).isEqualTo("Ana Costa");
        assertThat(resumo.get(0).getTotalAcoes()).isEqualTo(3);
        assertThat(resumo.get(0).getUltimaAcao()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
    }

    // --- eventosDeAuditLog() — Auditoria C06/C07 ---------------------------

    @Test
    void listarLogs_incluiEventosDaTabelaAuditLogComResultadoEMotivo() {
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        AuditLog loginFalhado = new AuditLog();
        loginFalhado.setId(1L);
        loginFalhado.setUtilizadorNome("intruso@exemplo.com");
        loginFalhado.setAcao("LOGIN");
        loginFalhado.setEntidade("User");
        loginFalhado.setResultado(AuditLogService.FALHA);
        loginFalhado.setMotivo("Password inválida");
        loginFalhado.setIp("203.0.113.7");
        loginFalhado.setTimestamp(LocalDateTime.of(2026, 8, 19, 10, 0));
        when(auditLogRepository.findAll()).thenReturn(List.of(loginFalhado));

        PaginaAuditoriaDTO pagina = service.listarLogs(null, null);

        assertThat(pagina.getItens()).hasSize(1);
        LogAuditoriaDTO evento = pagina.getItens().get(0);
        assertThat(evento.getUtilizador()).isEqualTo("intruso@exemplo.com");
        assertThat(evento.getResultado()).isEqualTo(AuditLogService.FALHA);
        assertThat(evento.getMotivo()).isEqualTo("Password inválida");
        assertThat(evento.getIp()).isEqualTo("203.0.113.7");
    }

    private DocumentoContabilistico documento(Long id, Long userId, String nome, LocalDateTime dataUpload) {
        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setId(id);
        documento.setUserId(userId);
        documento.setNomeFicheiro(nome);
        documento.setDataUpload(dataUpload);
        return documento;
    }

    private Lancamento lancamento(Long id, OrigemLancamento origem, Long validadoPor, LocalDateTime criadoEm) {
        Lancamento lancamento = new Lancamento();
        lancamento.setId(id);
        lancamento.setOrigem(origem);
        lancamento.setValidadoPor(validadoPor);
        // Auditoria C01: o evento de criação de um lançamento MANUAL passou
        // a atribuir-se a criadoPor, não a validadoPor (ver
        // AuditoriaService.autorDaCriacao) — este helper continua a servir
        // "um lançamento atribuído a esta pessoa" para os dois casos,
        // gravando o mesmo id nos dois campos.
        lancamento.setCriadoPor(validadoPor);
        lancamento.setCriadoEm(criadoEm);
        lancamento.setData(criadoEm.toLocalDate());
        lancamento.setDescricao("Lançamento " + id);
        lancamento.setEstado(EstadoLancamento.VALIDADO);
        return lancamento;
    }
}
