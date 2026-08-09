package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.CategoriaContaDTO;
import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.service.PlanoContasClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Peça 1 do bloco "Categorização do plano de contas": confirma que todos os
 * códigos referenciados pelas categorias existem mesmo no plano de contas
 * real (senão PlanoContasClient.porCodigo lançaria IllegalStateException) e
 * que a estrutura principais/ocasionais está correcta para os casos mais
 * usados nesta sessão (Vendas/Compras/IVA). PlanoContasClient mockado — só
 * precisa de devolver *algum* ContaDTO para cada código pedido, o nome não é
 * o que este teste verifica (isso é responsabilidade de test_pgc.py/
 * PlanoContasClient, não desta camada de agrupamento).
 */
class CategoriaContaControllerTest {

    private final PlanoContasClient planoContasClient = Mockito.mock(PlanoContasClient.class);
    private final CategoriaContaController controller = new CategoriaContaController(planoContasClient);

    {
        when(planoContasClient.porCodigo(anyString()))
                .thenAnswer((InvocationOnMock inv) -> {
                    String codigo = inv.getArgument(0);
                    return new ContaDTO(codigo, "Conta " + codigo);
                });
    }

    @Test
    void listar_todosOsCodigosReferenciadosExistemNoPlanoDeContas() {
        assertThatCode(controller::listar).doesNotThrowAnyException();
    }

    @Test
    void listar_devolveAsCategoriasEsperadas() {
        List<CategoriaContaDTO> categorias = controller.listar().getBody();

        assertThat(categorias)
                .extracting(CategoriaContaDTO::nome)
                .containsExactly("Vendas", "Compras", "Tesouraria", "Bancos", "Caixa", "Impostos", "Operações Diversas");
    }

    @Test
    void listar_vendas_temClientesVendasEIvaLiquidadoComoPrincipais() {
        CategoriaContaDTO vendas = categoriaPorNome("Vendas");

        assertThat(vendas.principais())
                .extracting(c -> c.getCodigo())
                .contains("31", "61", "34.5.2");
    }

    @Test
    void listar_compras_temFornecedoresEIvaDedutivelComoPrincipais() {
        CategoriaContaDTO compras = categoriaPorNome("Compras");

        assertThat(compras.principais())
                .extracting(c -> c.getCodigo())
                .contains("32", "21", "34.5.1");
    }

    @Test
    void listar_caixaEBancos_partilhamAsMesmasDuasContasEmPapeisTrocados() {
        CategoriaContaDTO caixa = categoriaPorNome("Caixa");
        CategoriaContaDTO bancos = categoriaPorNome("Bancos");

        assertThat(caixa.principais()).extracting(c -> c.getCodigo()).containsExactly("45");
        assertThat(caixa.ocasionais()).extracting(c -> c.getCodigo()).containsExactly("43");
        assertThat(bancos.principais()).extracting(c -> c.getCodigo()).containsExactly("43");
        assertThat(bancos.ocasionais()).extracting(c -> c.getCodigo()).containsExactly("45");
    }

    private CategoriaContaDTO categoriaPorNome(String nome) {
        return controller.listar().getBody().stream()
                .filter(c -> c.nome().equals(nome))
                .findFirst()
                .orElseThrow();
    }
}
