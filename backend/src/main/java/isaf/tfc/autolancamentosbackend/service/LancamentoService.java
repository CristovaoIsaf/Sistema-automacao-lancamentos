package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;

import java.time.LocalDate;
import java.util.List;

public interface LancamentoService {

    /**
     * Regista um lançamento contabilístico manual. `criadoPor` (Fase 7 do
     * plano de 20 fases) é o utilizador autenticado que o registou — antes
     * desta fase, Lancamento.validadoPor ficava sempre null para origem
     * MANUAL, uma lacuna de rastreabilidade/auditoria (só os lançamentos
     * AUTOMATICO, aprovados via AnaliseContabilService, guardavam quem os
     * validou).
     */
    LancamentoResponseDTO criarLancamentoManual(
            LancamentoRequestDTO request,
            Long criadoPor
    );


    /**
     * Obtém um lançamento pelo seu identificador.
     */
    LancamentoResponseDTO buscarPorId(Long id);


    /**
     * Lista todos os lançamentos.
     */
    List<LancamentoResponseDTO> listarTodos();


    /**
     * Lista lançamentos dentro de um período.
     */
    List<LancamentoResponseDTO> listarPorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim
    );


    /**
     * Lista lançamentos dentro de um período, opcionalmente filtrando por estado.
     * Usado pela exportação (estado null = todos os estados).
     */
    List<LancamentoResponseDTO> listarPorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim,
            EstadoLancamento estado
    );


    /**
     * Atualiza um lançamento ainda não fechado. `alteradoPor` (Fase 16 —
     * "auditor: alterações") é o utilizador autenticado que editou, para a
     * auditoria conseguir atribuir a alteração a alguém.
     */
    LancamentoResponseDTO atualizar(
            Long id,
            LancamentoRequestDTO request,
            Long alteradoPor
    );


    /**
     * Cancela um lançamento contabilístico. `alteradoPor` — ver atualizar().
     */
    void cancelar(Long id, Long alteradoPor);
}