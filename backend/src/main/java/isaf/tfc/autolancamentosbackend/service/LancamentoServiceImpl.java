package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.service.LancamentoService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LancamentoServiceImpl implements LancamentoService {


    private final LancamentoRepository repository;
    // Fase 9 — único ponto de conversão Lancamento→DTO, partilhado com
    // AnaliseContabilService (ver LancamentoEnriquecimentoService).
    private final LancamentoEnriquecimentoService enriquecimentoService;


    @Override
    @Transactional
    public LancamentoResponseDTO criarLancamentoManual(
            LancamentoRequestDTO request,
            Long criadoPor
    ) {


        // 1. Criar lançamento
        Lancamento lancamento =
                new Lancamento();


        lancamento.setData(request.getData());

        lancamento.setDescricao(
                request.getDescricao()
        );

        lancamento.setValidadoPor(criadoPor);


        // VALIDADO logo na criação: ao contrário de uma Sugestao da IA (que
        // precisa de revisão humana antes de virar Lancamento — ver
        // AnaliseContabilService.aprovarSugestao), um lançamento manual já
        // É a decisão humana direta. Ficar PENDENTE para sempre (sem
        // nenhum endpoint que o mude de estado) fazia-o nunca aparecer no
        // Balancete/Dashboard.
        lancamento.setEstado(
                EstadoLancamento.VALIDADO
        );


        lancamento.setOrigem(
                OrigemLancamento.MANUAL
        );


        // 2. Construir linhas (mesmo domínio partilhado com
        // AnaliseContabilService — ver PartidasDobradas, Fase 7) e validar
        // equilíbrio ANTES de associar ao lançamento a gravar.
        List<LinhaLancamento> linhas =
                PartidasDobradas.construirLinhas(request.getLinhas(), request.getDescricao());

        PartidasDobradas.validarEquilibrio(linhas);

        linhas.forEach(linha -> {
            linha.setLancamento(lancamento);
            lancamento.getLinhas().add(linha);
        });


        // 3. Guardar
        Lancamento salvo =
                repository.save(lancamento);


        return enriquecimentoService.converter(salvo);
    }

    @Override
    public LancamentoResponseDTO buscarPorId(Long id) {

        Lancamento lancamento =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Lançamento não encontrado")
                        );

        return enriquecimentoService.converter(lancamento);
    }


    @Override
    public List<LancamentoResponseDTO> listarTodos() {

        return enriquecimentoService.converterTodos(repository.findAll());
    }


    @Override
    public List<LancamentoResponseDTO> listarPorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim
    ) {

        return enriquecimentoService.converterTodos(
                repository.findByDataBetween(dataInicio, dataFim)
        );
    }


    @Override
    public List<LancamentoResponseDTO> listarPorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim,
            EstadoLancamento estado
    ) {

        List<Lancamento> lancamentos = repository
                .findByDataBetween(dataInicio, dataFim)
                .stream()
                .filter(lancamento -> estado == null || lancamento.getEstado() == estado)
                .toList();

        return enriquecimentoService.converterTodos(lancamentos);
    }


    @Override
    @Transactional
    public LancamentoResponseDTO atualizar(
            Long id,
            LancamentoRequestDTO request
    ) {

        Lancamento lancamento =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Lançamento não encontrado")
                        );


        lancamento.setDescricao(request.getDescricao());
        lancamento.setData(request.getData());


        return enriquecimentoService.converter(
                repository.save(lancamento)
        );
    }


    @Override
    @Transactional
    public void cancelar(Long id) {

        Lancamento lancamento =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Lançamento não encontrado")
                        );


        lancamento.setEstado(EstadoLancamento.CANCELADO);

        repository.save(lancamento);
    }
}