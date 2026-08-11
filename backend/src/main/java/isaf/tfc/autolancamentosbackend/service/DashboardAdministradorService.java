package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.DashboardAdministradorDTO;
import isaf.tfc.autolancamentosbackend.dto.PendenciaDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoSugestao;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;

/**
 * Fase 15 do plano de 20 fases — "o administrador deve possuir visão da
 * empresa": compõe (nunca recalcula) os dados exclusivos do Administrador
 * a partir de serviços já existentes — AuditoriaService (Fase 15,
 * "acompanhar contabilistas") e SugestaoRepository ("visualizar
 * pendências"). "Consultar utilizadores/documentos/lançamentos" já têm
 * páginas próprias (Utilizadores, Documentos, Lançamentos) — não
 * duplicadas aqui, só ligadas a partir da visão de Administrador.
 */
@Service
public class DashboardAdministradorService {

    private final AuditoriaService auditoriaService;
    private final SugestaoRepository sugestaoRepository;

    public DashboardAdministradorService(AuditoriaService auditoriaService, SugestaoRepository sugestaoRepository) {
        this.auditoriaService = auditoriaService;
        this.sugestaoRepository = sugestaoRepository;
    }

    public DashboardAdministradorDTO obter() {
        var pendencias = sugestaoRepository.findAll().stream()
                .filter(s -> s.getEstado() == EstadoSugestao.PENDENTE)
                .sorted(Comparator.comparing(Sugestao::getDataCriacao, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::converterPendencia)
                .toList();

        return new DashboardAdministradorDTO(auditoriaService.resumoPorUtilizador(), pendencias);
    }

    private PendenciaDTO converterPendencia(Sugestao sugestao) {
        return new PendenciaDTO(
                sugestao.getId(),
                sugestao.getDocumentoId(),
                sugestao.getDescricao(),
                sugestao.getEntidade(),
                sugestao.getValor(),
                sugestao.getDataCriacao()
        );
    }
}
