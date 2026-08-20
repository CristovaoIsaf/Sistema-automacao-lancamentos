package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoHistoricoDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;

import java.time.LocalDate;
import java.util.List;

public interface LancamentoService {

    /**
     * Regista um lançamento contabilístico manual como PENDENTE — ainda
     * não conta para relatórios até um segundo contabilista o aprovar (ver
     * aprovar()). Auditoria C01: antes desta correção nascia já VALIDADO,
     * aprovado pelo próprio criador na mesma chamada — sem segregação
     * nenhuma entre "quem lança" e "quem aprova".
     */
    LancamentoResponseDTO criarLancamentoManual(
            LancamentoRequestDTO request,
            Long criadoPor
    );

    /**
     * Aprova um lançamento manual PENDENTE, passando-o a VALIDADO — só
     * depois disto entra em Balancete/DRE/Balanço/Fluxo de Caixa/Livro
     * Razão. `aprovadoPor` tem de ser diferente de quem criou (ver
     * Lancamento.criadoPor) — auto-aprovação continua bloqueada mesmo que
     * alguém chame este endpoint diretamente.
     */
    LancamentoResponseDTO aprovar(Long id, Long aprovadoPor);


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
     * Auditoria C04 — todas as versões anteriores deste lançamento (uma por
     * cada edição feita via atualizar()), mais recente primeiro.
     */
    List<LancamentoHistoricoDTO> listarHistorico(Long lancamentoId);


    /**
     * Auditoria C03: pedido de anulação de um lançamento VALIDADO — precisa
     * de motivo e de um segundo contabilista aprovar (ver
     * aprovarCancelamento) antes de ter qualquer efeito contabilístico.
     */
    LancamentoResponseDTO solicitarCancelamento(Long id, String motivo, Long solicitadoPor);

    /**
     * Aprova um pedido de anulação: marca o lançamento original como
     * CANCELADO e gera um lançamento de estorno (linhas invertidas) com o
     * mesmo valor, para o efeito contabilístico ficar sempre rastreável —
     * nunca um simples "desaparecimento" do lançamento original.
     * `aprovadoPor` tem de ser diferente de quem pediu a anulação.
     */
    LancamentoResponseDTO aprovarCancelamento(Long id, Long aprovadoPor);

    /**
     * Rejeita um pedido de anulação: o lançamento volta a VALIDADO, sem
     * nenhum estorno gerado. `rejeitadoPor` tem de ser diferente de quem
     * pediu a anulação, tal como aprovarCancelamento.
     */
    LancamentoResponseDTO rejeitarCancelamento(Long id, Long rejeitadoPor);
}
