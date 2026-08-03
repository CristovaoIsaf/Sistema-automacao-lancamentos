package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;

import java.time.LocalDate;
import java.util.List;

public interface LancamentoService {

    /**
     * Regista um lançamento contabilístico manual.
     */
    LancamentoResponseDTO criarLancamentoManual(
            LancamentoRequestDTO request
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
     * Atualiza um lançamento ainda não fechado.
     */
    LancamentoResponseDTO atualizar(
            Long id,
            LancamentoRequestDTO request
    );


    /**
     * Cancela um lançamento contabilístico.
     */
    void cancelar(Long id);
}