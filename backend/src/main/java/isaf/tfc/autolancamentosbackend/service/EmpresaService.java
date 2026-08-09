package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.EmpresaDTO;
import isaf.tfc.autolancamentosbackend.model.Empresa;
import isaf.tfc.autolancamentosbackend.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

/**
 * Âmbito deste TFC: uma única empresa por instalação (decisão registada ao
 * decidir a secção 4 do documento de retoma) — por isso não há criação de
 * novas empresas aqui, só consulta/edição do único registo já semeado.
 */
@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public EmpresaDTO obterEmpresa() {
        return converterParaDTO(buscarUnica());
    }

    // Fase 1 — modelo de empresa/utilizadores: usado por UserService.criar
    // para ligar cada novo utilizador ao contexto da empresa única desta
    // instalação (ver nota em User.empresaId).
    public Long idDaEmpresaUnica() {
        return buscarUnica().getId();
    }

    public EmpresaDTO atualizarEmpresa(EmpresaDTO dados) {
        Empresa empresa = buscarUnica();

        empresa.setNome(dados.getNome());
        empresa.setNif(dados.getNif());
        empresa.setEmail(dados.getEmail());
        empresa.setEndereco(dados.getEndereco());
        empresa.setTelefone(dados.getTelefone());
        // Fase 2 — contexto contabilístico, ver nota FACTO/CONTEXTO em Empresa.
        empresa.setAtividadeEconomica(dados.getAtividadeEconomica());
        empresa.setNaturezaNegocio(dados.getNaturezaNegocio());
        empresa.setMoeda(dados.getMoeda());
        empresa.setExercicioAtualInicio(dados.getExercicioAtualInicio());
        empresa.setExercicioAtualFim(dados.getExercicioAtualFim());

        return converterParaDTO(empresaRepository.save(empresa));
    }

    private Empresa buscarUnica() {
        return empresaRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Nenhuma empresa configurada."));
    }

    private EmpresaDTO converterParaDTO(Empresa empresa) {
        return new EmpresaDTO(
                empresa.getId(),
                empresa.getNome(),
                empresa.getNif(),
                empresa.getEmail(),
                empresa.getEndereco(),
                empresa.getTelefone(),
                empresa.getAtividadeEconomica(),
                empresa.getNaturezaNegocio(),
                empresa.getMoeda(),
                empresa.getExercicioAtualInicio(),
                empresa.getExercicioAtualFim()
        );
    }
}
