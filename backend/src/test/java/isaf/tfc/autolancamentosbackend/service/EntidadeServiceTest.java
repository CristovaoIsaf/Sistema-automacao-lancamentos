package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.TipoEntidade;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Peça "EntidadeService" do desenvolvimento incremental — testa isoladamente
 * (EntidadeRepository mockado) a resolução/criação de entidades (T2 —
 * arquivo por entidade) a partir do NIF/nome/tipoDocumento devolvidos pela
 * análise.
 */
class EntidadeServiceTest {

    private EntidadeRepository entidadeRepository;
    private EntidadeService service;

    @BeforeEach
    void setUp() {
        entidadeRepository = Mockito.mock(EntidadeRepository.class);
        service = new EntidadeService(entidadeRepository);
    }

    @Test
    void resolverOuCriar_semNif_usaEntidadePartilhadaSemIdentificacao() {
        when(entidadeRepository.findByNif("SEM_NIF")).thenReturn(Optional.empty());
        when(entidadeRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        Entidade resultado = service.resolverOuCriar(null, "Qualquer Nome", "compra_mercadoria");

        assertThat(resultado.getNif()).isEqualTo("SEM_NIF");
        assertThat(resultado.getNome()).isEqualTo("Sem identificação");
        assertThat(resultado.getTipo()).isEqualTo(TipoEntidade.DESCONHECIDO);
    }

    @Test
    void resolverOuCriar_nifEmBranco_tratadoComoSemIdentificacao() {
        when(entidadeRepository.findByNif("SEM_NIF")).thenReturn(Optional.empty());
        when(entidadeRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        Entidade resultado = service.resolverOuCriar("   ", "Nome", "venda_mercadoria");

        assertThat(resultado.getNif()).isEqualTo("SEM_NIF");
        assertThat(resultado.getTipo()).isEqualTo(TipoEntidade.DESCONHECIDO);
    }

    @Test
    void resolverOuCriar_nifNovoTipoCompra_criaComoFornecedor() {
        when(entidadeRepository.findByNif("5417002619")).thenReturn(Optional.empty());
        when(entidadeRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        Entidade resultado = service.resolverOuCriar("5417002619", "Sonangol Distribuidora Lda", "compra_servico");

        assertThat(resultado.getNif()).isEqualTo("5417002619");
        assertThat(resultado.getNome()).isEqualTo("Sonangol Distribuidora Lda");
        assertThat(resultado.getTipo()).isEqualTo(TipoEntidade.FORNECEDOR);
    }

    @Test
    void resolverOuCriar_nifNovoTipoVenda_criaComoCliente() {
        when(entidadeRepository.findByNif("5417099002")).thenReturn(Optional.empty());
        when(entidadeRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        Entidade resultado = service.resolverOuCriar("5417099002", "Mercado Central, Lda", "prestacao_servico");

        assertThat(resultado.getTipo()).isEqualTo(TipoEntidade.CLIENTE);
    }

    @Test
    void resolverOuCriar_tipoDocumentoDesconhecido_criaComoDesconhecido() {
        when(entidadeRepository.findByNif("5417002619")).thenReturn(Optional.empty());
        when(entidadeRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        Entidade resultado = service.resolverOuCriar("5417002619", "Alguém", "a_classificar");

        assertThat(resultado.getTipo()).isEqualTo(TipoEntidade.DESCONHECIDO);
    }

    @Test
    void resolverOuCriar_nifJaConhecido_reutilizaEntidadeExistenteSemCriarNova() {
        Entidade existente = new Entidade(9L, "Sonangol Distribuidora Lda", "5417002619", TipoEntidade.FORNECEDOR);
        when(entidadeRepository.findByNif("5417002619")).thenReturn(Optional.of(existente));

        Entidade resultado = service.resolverOuCriar("5417002619", "Nome diferente desta vez", "compra_mercadoria");

        assertThat(resultado).isSameAs(existente);
        verify(entidadeRepository, never()).save(Mockito.any());
    }

    @Test
    void resolverOuCriar_nifComEspacos_ficaTrimado() {
        when(entidadeRepository.findByNif("5417002619")).thenReturn(Optional.empty());
        when(entidadeRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        service.resolverOuCriar("  5417002619  ", "Nome", "compra_mercadoria");

        ArgumentCaptor<Entidade> captor = ArgumentCaptor.forClass(Entidade.class);
        verify(entidadeRepository).save(captor.capture());
        assertThat(captor.getValue().getNif()).isEqualTo("5417002619");
    }
}
