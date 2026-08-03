package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.service.LancamentoService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LancamentoServiceImpl implements LancamentoService {


    private final LancamentoRepository repository;


    @Override
    @Transactional
    public LancamentoResponseDTO criarLancamentoManual(
            LancamentoRequestDTO request
    ) {


        // 1. Validar antes de criar
        validarEquilibrio(request.getLinhas());


        // 2. Criar lançamento
        Lancamento lancamento =
                new Lancamento();


        lancamento.setData(request.getData());

        lancamento.setDescricao(
                request.getDescricao()
        );


        lancamento.setEstado(
                EstadoLancamento.PENDENTE
        );


        lancamento.setOrigem(
                OrigemLancamento.MANUAL
        );


        // 3. Criar linhas
        request.getLinhas()
                .forEach(linhaDTO -> {

                    LinhaLancamento linha =
                            new LinhaLancamento();

                    linha.setConta(
                            linhaDTO.getConta()
                    );

                    linha.setDebito(
                            linhaDTO.getDebito()
                    );

                    linha.setCredito(
                            linhaDTO.getCredito()
                    );

                    linha.setLancamento(lancamento);


                    lancamento.getLinhas()
                            .add(linha);
                });


        // 4. Guardar
        Lancamento salvo =
                repository.save(lancamento);


        return converterParaDTO(salvo);
    }

    @Override
    public LancamentoResponseDTO buscarPorId(Long id) {

        Lancamento lancamento =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Lançamento não encontrado")
                        );

        return converterParaDTO(lancamento);
    }


    @Override
    public List<LancamentoResponseDTO> listarTodos() {

        return repository.findAll()
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }


    @Override
    public List<LancamentoResponseDTO> listarPorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim
    ) {

        return repository
                .findByDataBetween(dataInicio, dataFim)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }


    @Override
    public List<LancamentoResponseDTO> listarPorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim,
            EstadoLancamento estado
    ) {

        return repository
                .findByDataBetween(dataInicio, dataFim)
                .stream()
                .filter(lancamento -> estado == null || lancamento.getEstado() == estado)
                .map(this::converterParaDTO)
                .toList();
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


        return converterParaDTO(
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



    private LancamentoResponseDTO converterParaDTO(
            Lancamento lancamento
    ){

        LancamentoResponseDTO dto =
                new LancamentoResponseDTO();


        dto.setId(lancamento.getId());

        dto.setData(lancamento.getData());

        dto.setDescricao(lancamento.getDescricao());

        dto.setEstado(lancamento.getEstado());

        dto.setOrigem(lancamento.getOrigem());


        List<LinhaLancamentoDTO> linhas =
                lancamento.getLinhas()
                        .stream()
                        .map(linha -> new LinhaLancamentoDTO(
                                linha.getConta(),
                                linha.getDebito(),
                                linha.getCredito(),
                                linha.getDescricao()
                        ))
                        .toList();


        dto.setLinhas(linhas);


        return dto;
    }
    private void validarEquilibrio(
            List<LinhaLancamentoDTO> linhas
    ){

        BigDecimal totalDebito =
                linhas.stream()
                        .map(LinhaLancamentoDTO::getDebito)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal totalCredito =
                linhas.stream()
                        .map(LinhaLancamentoDTO::getCredito)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);


        if(totalDebito.compareTo(totalCredito) != 0){

            throw new RuntimeException(
                    "O lançamento não está equilibrado"
            );
        }

    }
}