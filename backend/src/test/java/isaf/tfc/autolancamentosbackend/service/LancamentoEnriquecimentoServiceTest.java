package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.TipoEntidade;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Fase 9 do plano de 20 fases: LancamentoEnriquecimentoService é o único
 * ponto que resolve entidade/utilizador para o histórico (usado tanto por
 * LancamentoServiceImpl — origem MANUAL — como por AnaliseContabilService
 * — origem AUTOMATICO). Cobre a cadeia
 * Lancamento.sugestaoId → Sugestao.documentoId → Documento.entidadeId →
 * Entidade, e Lancamento.validadoPor → User, incluindo o caso MANUAL (sem
 * sugestaoId, sem entidade).
 */
class LancamentoEnriquecimentoServiceTest {

    private SugestaoRepository sugestaoRepository;
    private DocumentoRepository documentoRepository;
    private EntidadeRepository entidadeRepository;
    private UserRepository userRepository;
    private LancamentoEnriquecimentoService service;

    @BeforeEach
    void setUp() {
        sugestaoRepository = Mockito.mock(SugestaoRepository.class);
        documentoRepository = Mockito.mock(DocumentoRepository.class);
        entidadeRepository = Mockito.mock(EntidadeRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        service = new LancamentoEnriquecimentoService(sugestaoRepository, documentoRepository, entidadeRepository, userRepository);
    }

    @Test
    void converter_lancamentoAutomatico_resolveEntidadeAtravesDaSugestaoEDoDocumento() {
        Sugestao sugestao = new Sugestao();
        sugestao.setId(10L);
        sugestao.setDocumentoId(20L);
        when(sugestaoRepository.findAllById(List.of(10L))).thenReturn(List.of(sugestao));

        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setId(20L);
        documento.setEntidadeId(30L);
        when(documentoRepository.findAllById(List.of(20L))).thenReturn(List.of(documento));

        Entidade entidade = new Entidade(30L, "Fornecedor XPTO", "5417000111", TipoEntidade.FORNECEDOR);
        when(entidadeRepository.findAllById(List.of(30L))).thenReturn(List.of(entidade));

        User validador = new User(1L, "Ana Costa", "ana@exemplo.com", "5000000001", "ATIVO", "hash", Role.CONTABILISTA, null);
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(validador));

        Lancamento lancamento = new Lancamento();
        lancamento.setId(99L);
        lancamento.setSugestaoId(10L);
        lancamento.setValidadoPor(1L);
        lancamento.setData(LocalDate.now());
        lancamento.setDescricao("Compra de mercadoria");
        lancamento.setEstado(EstadoLancamento.VALIDADO);
        lancamento.setOrigem(OrigemLancamento.AUTOMATICO);

        LancamentoResponseDTO dto = service.converter(lancamento);

        assertThat(dto.getDocumentoId()).isEqualTo(20L);
        assertThat(dto.getEntidadeId()).isEqualTo(30L);
        assertThat(dto.getEntidadeNome()).isEqualTo("Fornecedor XPTO");
        assertThat(dto.getValidadoPor()).isEqualTo(1L);
        assertThat(dto.getValidadoPorNome()).isEqualTo("Ana Costa");
    }

    @Test
    void converter_lancamentoManual_semSugestao_ficaSemEntidadeMasComUtilizador() {
        User criador = new User(2L, "Bruno Neto", "bruno@exemplo.com", "5000000002", "ATIVO", "hash", Role.CONTABILISTA, null);
        when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(criador));
        when(sugestaoRepository.findAllById(List.of())).thenReturn(List.of());

        Lancamento lancamento = new Lancamento();
        lancamento.setId(100L);
        lancamento.setValidadoPor(2L);
        lancamento.setData(LocalDate.now());
        lancamento.setDescricao("Pagamento de renda");
        lancamento.setEstado(EstadoLancamento.VALIDADO);
        lancamento.setOrigem(OrigemLancamento.MANUAL);

        LancamentoResponseDTO dto = service.converter(lancamento);

        assertThat(dto.getDocumentoId()).isNull();
        assertThat(dto.getEntidadeId()).isNull();
        assertThat(dto.getEntidadeNome()).isNull();
        assertThat(dto.getValidadoPorNome()).isEqualTo("Bruno Neto");
    }
}
