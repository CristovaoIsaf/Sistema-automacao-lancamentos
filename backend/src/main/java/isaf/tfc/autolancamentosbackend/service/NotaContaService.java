package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.GrupoEntidadeDTO;
import isaf.tfc.autolancamentosbackend.dto.MovimentoContaDTO;
import isaf.tfc.autolancamentosbackend.dto.NotaContaResponseDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * T3 — Notas às contas: para uma conta do PGC-AO, mostra que movimentos
 * compõem o seu saldo num período, agrupados pela entidade de origem
 * (cliente/fornecedor), usando a rastreabilidade já construída na T2
 * (Documento → Sugestao → Lancamento → Entidade).
 */
@Service
public class NotaContaService {

    // Cobre tanto lançamentos manuais (sem Sugestao/Documento por natureza)
    // como lançamentos automáticos anteriores à T2, que não têm o rasto
    // documento→entidade preenchido retroactivamente.
    private static final String SEM_ORIGEM_DOCUMENTAL = "Sem origem documental associada";

    private final LancamentoRepository lancamentoRepository;
    private final SugestaoRepository sugestaoRepository;
    private final DocumentoRepository documentoRepository;
    private final EntidadeRepository entidadeRepository;

    public NotaContaService(LancamentoRepository lancamentoRepository,
                             SugestaoRepository sugestaoRepository,
                             DocumentoRepository documentoRepository,
                             EntidadeRepository entidadeRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.sugestaoRepository = sugestaoRepository;
        this.documentoRepository = documentoRepository;
        this.entidadeRepository = entidadeRepository;
    }

    public NotaContaResponseDTO obterNota(String conta, LocalDate inicio, LocalDate fim) {
        List<Lancamento> lancamentos = lancamentoRepository.findAll().stream()
                .filter(l -> l.getData() != null && dentroDoIntervalo(l.getData(), inicio, fim))
                .toList();

        // Um Lancamento nasce de, no máximo, uma Sugestao aprovada — pré-carregar
        // por lancamentoId evita uma query por linha dentro do ciclo abaixo.
        Map<Long, Sugestao> sugestaoPorLancamento = sugestaoRepository.findAll().stream()
                .filter(s -> s.getLancamentoId() != null)
                .collect(Collectors.toMap(Sugestao::getLancamentoId, s -> s, (a, b) -> a));

        Map<Long, DocumentoContabilistico> documentoPorId = documentoRepository.findAll().stream()
                .collect(Collectors.toMap(DocumentoContabilistico::getId, d -> d));

        Map<Long, Entidade> entidadePorId = entidadeRepository.findAll().stream()
                .collect(Collectors.toMap(Entidade::getId, e -> e));

        Map<String, List<MovimentoContaDTO>> movimentosPorEntidade = new LinkedHashMap<>();
        Map<String, String> tipoPorEntidade = new HashMap<>();

        for (Lancamento lancamento : lancamentos) {
            for (LinhaLancamento linha : lancamento.getLinhas()) {
                if (!contaCorresponde(linha.getConta(), conta)) {
                    continue;
                }

                Sugestao sugestao = sugestaoPorLancamento.get(lancamento.getId());
                DocumentoContabilistico documento = (sugestao != null && sugestao.getDocumentoId() != null)
                        ? documentoPorId.get(sugestao.getDocumentoId())
                        : null;
                Entidade entidade = (documento != null && documento.getEntidadeId() != null)
                        ? entidadePorId.get(documento.getEntidadeId())
                        : null;

                String nomeGrupo = entidade != null ? entidade.getNome() : SEM_ORIGEM_DOCUMENTAL;
                String tipoGrupo = (entidade != null && entidade.getTipo() != null) ? entidade.getTipo().name() : null;

                MovimentoContaDTO movimento = new MovimentoContaDTO(
                        lancamento.getId(),
                        lancamento.getData(),
                        lancamento.getDescricao(),
                        documento != null ? documento.getId() : null,
                        documento != null ? documento.getNomeFicheiro() : null,
                        linha.getDebito(),
                        linha.getCredito()
                );

                movimentosPorEntidade.computeIfAbsent(nomeGrupo, k -> new ArrayList<>()).add(movimento);
                tipoPorEntidade.putIfAbsent(nomeGrupo, tipoGrupo);
            }
        }

        List<GrupoEntidadeDTO> grupos = new ArrayList<>();
        BigDecimal totalDebito = BigDecimal.ZERO;
        BigDecimal totalCredito = BigDecimal.ZERO;

        for (Map.Entry<String, List<MovimentoContaDTO>> entrada : movimentosPorEntidade.entrySet()) {
            BigDecimal subDebito = somar(entrada.getValue(), MovimentoContaDTO::getDebito);
            BigDecimal subCredito = somar(entrada.getValue(), MovimentoContaDTO::getCredito);

            grupos.add(new GrupoEntidadeDTO(
                    entrada.getKey(), tipoPorEntidade.get(entrada.getKey()), entrada.getValue(), subDebito, subCredito));

            totalDebito = totalDebito.add(subDebito);
            totalCredito = totalCredito.add(subCredito);
        }

        return new NotaContaResponseDTO(conta, inicio, fim, grupos, totalDebito, totalCredito, totalDebito.subtract(totalCredito));
    }

    private boolean contaCorresponde(String contaLinha, String contaConsultada) {
        if (contaLinha == null) {
            return false;
        }
        // Permite consultar tanto a conta exacta ("32") como todas as
        // sub-contas de uma conta-mãe ("75" também apanha "75.2.23").
        return contaLinha.equals(contaConsultada) || contaLinha.startsWith(contaConsultada + ".");
    }

    private boolean dentroDoIntervalo(LocalDate data, LocalDate inicio, LocalDate fim) {
        if (inicio != null && data.isBefore(inicio)) {
            return false;
        }
        return fim == null || !data.isAfter(fim);
    }

    private BigDecimal somar(List<MovimentoContaDTO> movimentos, java.util.function.Function<MovimentoContaDTO, BigDecimal> campo) {
        return movimentos.stream()
                .map(campo)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
