package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.EmpresaDTO;
import isaf.tfc.autolancamentosbackend.model.Empresa;
import isaf.tfc.autolancamentosbackend.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Peça "EmpresaService" do desenvolvimento incremental — testa isoladamente
 * (EmpresaRepository mockado) a leitura/edição do registo único de empresa
 * (âmbito single-tenant deste TFC, ver Javadoc da classe).
 */
class EmpresaServiceTest {

    private EmpresaRepository empresaRepository;
    private EmpresaService service;

    @BeforeEach
    void setUp() {
        empresaRepository = Mockito.mock(EmpresaRepository.class);
        service = new EmpresaService(empresaRepository);
    }

    private Empresa empresaSemeada() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNome("Empresa Teste TFC, Lda");
        empresa.setNif("5417002619");
        empresa.setMoeda("AOA");
        return empresa;
    }

    @Test
    void obterEmpresa_devolveOUnicoRegistoConvertidoParaDTO() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaSemeada()));

        EmpresaDTO dto = service.obterEmpresa();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNome()).isEqualTo("Empresa Teste TFC, Lda");
        assertThat(dto.getNif()).isEqualTo("5417002619");
        assertThat(dto.getMoeda()).isEqualTo("AOA");
    }

    @Test
    void obterEmpresa_semNenhumaEmpresaConfigurada_lancaExcecao() {
        when(empresaRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.obterEmpresa())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Nenhuma empresa configurada");
    }

    @Test
    void idDaEmpresaUnica_devolveOIdDoUnicoRegisto() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaSemeada()));

        assertThat(service.idDaEmpresaUnica()).isEqualTo(1L);
    }

    @Test
    void atualizarEmpresa_gravaTodosOsCamposIncluindoContextoContabilistico() {
        Empresa existente = empresaSemeada();
        when(empresaRepository.findAll()).thenReturn(List.of(existente));
        when(empresaRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        EmpresaDTO dados = new EmpresaDTO(
                null, "Novo Nome", "5000000001LA", "novo@empresa.ao", "Rua X, Luanda", "+244900000000",
                "Comércio a retalho", "Revenda de mercadorias", "AOA",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        EmpresaDTO resultado = service.atualizarEmpresa(dados);

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(captor.capture());
        Empresa gravada = captor.getValue();

        assertThat(gravada.getNome()).isEqualTo("Novo Nome");
        assertThat(gravada.getNif()).isEqualTo("5000000001LA");
        assertThat(gravada.getEmail()).isEqualTo("novo@empresa.ao");
        assertThat(gravada.getEndereco()).isEqualTo("Rua X, Luanda");
        assertThat(gravada.getTelefone()).isEqualTo("+244900000000");
        assertThat(gravada.getAtividadeEconomica()).isEqualTo("Comércio a retalho");
        assertThat(gravada.getNaturezaNegocio()).isEqualTo("Revenda de mercadorias");
        assertThat(gravada.getMoeda()).isEqualTo("AOA");
        assertThat(gravada.getExercicioAtualInicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(gravada.getExercicioAtualFim()).isEqualTo(LocalDate.of(2026, 12, 31));

        // O registo continua a ser o MESMO (id=1), nunca cria uma segunda empresa.
        assertThat(gravada.getId()).isEqualTo(1L);
        assertThat(resultado.getNome()).isEqualTo("Novo Nome");
    }
}
