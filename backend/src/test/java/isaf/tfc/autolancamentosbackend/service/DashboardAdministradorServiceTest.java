package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.AtividadeUtilizadorDTO;
import isaf.tfc.autolancamentosbackend.dto.DashboardAdministradorDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoSugestao;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Fase 15 do plano de 20 fases — "visualizar pendências":
 * DashboardAdministradorService isolado (AuditoriaService mockado — já
 * tem cobertura própria em AuditoriaServiceTest).
 */
class DashboardAdministradorServiceTest {

    private AuditoriaService auditoriaService;
    private SugestaoRepository sugestaoRepository;
    private DashboardAdministradorService service;

    @BeforeEach
    void setUp() {
        auditoriaService = Mockito.mock(AuditoriaService.class);
        sugestaoRepository = Mockito.mock(SugestaoRepository.class);
        service = new DashboardAdministradorService(auditoriaService, sugestaoRepository);
    }

    @Test
    void obter_incluiSoSugestoesPendentesENuncaAprovadasOuRejeitadas() {
        when(auditoriaService.resumoPorUtilizador()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of(
                sugestaoComEstado(1L, EstadoSugestao.PENDENTE, LocalDateTime.of(2026, 8, 1, 10, 0)),
                sugestaoComEstado(2L, EstadoSugestao.APROVADA, LocalDateTime.of(2026, 8, 1, 9, 0)),
                sugestaoComEstado(3L, EstadoSugestao.REJEITADA, LocalDateTime.of(2026, 8, 1, 8, 0))
        ));

        DashboardAdministradorDTO dashboard = service.obter();

        assertThat(dashboard.getPendencias()).hasSize(1);
        assertThat(dashboard.getPendencias().get(0).getSugestaoId()).isEqualTo(1L);
    }

    @Test
    void obter_ordenaPendenciasPorDataDecrescente() {
        when(auditoriaService.resumoPorUtilizador()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of(
                sugestaoComEstado(1L, EstadoSugestao.PENDENTE, LocalDateTime.of(2026, 1, 1, 0, 0)),
                sugestaoComEstado(2L, EstadoSugestao.PENDENTE, LocalDateTime.of(2026, 8, 1, 0, 0))
        ));

        DashboardAdministradorDTO dashboard = service.obter();

        assertThat(dashboard.getPendencias()).extracting(p -> p.getSugestaoId())
                .containsExactly(2L, 1L);
    }

    @Test
    void obter_reutilizaOResumoDeAtividadeDoAuditoriaService() {
        List<AtividadeUtilizadorDTO> resumoEsperado = List.of(
                new AtividadeUtilizadorDTO("Ana Costa", "CONTABILISTA", 5, LocalDateTime.now())
        );
        when(auditoriaService.resumoPorUtilizador()).thenReturn(resumoEsperado);
        when(sugestaoRepository.findAll()).thenReturn(List.of());

        DashboardAdministradorDTO dashboard = service.obter();

        assertThat(dashboard.getAtividadePorUtilizador()).isEqualTo(resumoEsperado);
    }

    private Sugestao sugestaoComEstado(Long id, EstadoSugestao estado, LocalDateTime dataCriacao) {
        Sugestao sugestao = new Sugestao();
        sugestao.setId(id);
        sugestao.setEstado(estado);
        sugestao.setDataCriacao(dataCriacao);
        sugestao.setDescricao("Sugestão " + id);
        return sugestao;
    }
}
