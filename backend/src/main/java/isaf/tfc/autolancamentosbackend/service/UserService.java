package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.UserRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.UserResponseDTO;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Nota: esta classe já não implementa UserDetailsService — essa
 * responsabilidade é do AuthService. Ter as duas a implementar a mesma
 * interface causava o aviso "Found 2 UserDetailsService beans" no arranque
 * (e a implementação daqui devolvia sempre null, nunca foi usada).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaService empresaService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmpresaService empresaService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.empresaService = empresaService;
    }

    public List<UserResponseDTO> listar() {
        return userRepository.findAll().stream()
                .map(this::converterParaDTO)
                .toList();
    }

    public UserResponseDTO buscarPorId(Long id) {
        return converterParaDTO(buscarEntidade(id));
    }

    public UserResponseDTO criar(UserRequestDTO dados) {
        User user = new User();
        user.setNome(dados.getNome());
        user.setEmail(dados.getEmail());
        user.setNif(dados.getNif());
        user.setPapel(dados.getPapel());
        user.setStatus("ATIVO");
        user.setSenha(passwordEncoder.encode(dados.getSenha()));
        // Fase 1 — utilizador pertence ao contexto da empresa da instalação
        // (single-tenant: sempre a única empresa já semeada).
        user.setEmpresaId(empresaService.idDaEmpresaUnica());

        return converterParaDTO(userRepository.save(user));
    }

    public UserResponseDTO atualizar(Long id, UserRequestDTO dados) {
        User user = buscarEntidade(id);

        user.setNome(dados.getNome());
        user.setEmail(dados.getEmail());
        user.setNif(dados.getNif());
        user.setPapel(dados.getPapel());

        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            user.setSenha(passwordEncoder.encode(dados.getSenha()));
        }

        // Auditoria C09 — suspender/reativar um utilizador (ver
        // User.isEnabled(), AuthController.login, JwtAuthFilter). Em falta
        // ou vazio, mantém o status atual em vez de o apagar.
        if (dados.getStatus() != null && !dados.getStatus().isBlank()) {
            user.setStatus(dados.getStatus());
        }

        return converterParaDTO(userRepository.save(user));
    }

    // Auditoria C08: eliminação física de um utilizador não é segura — nem
    // Lancamento (criadoPor/validadoPor/alteradoPor/cancelamentoSolicitadoPor)
    // nem DocumentoContabilistico.userId têm FK para "users", por isso a
    // BD deixava apagar mesmo quando o utilizador tinha lançamentos/uploads
    // associados — o histórico ficava com o id órfão e a auditoria
    // derivada (ver AuditoriaService) passava a mostrar "Utilizador
    // desconhecido" para tudo o que essa pessoa alguma vez fez. Reaproveita
    // o campo status já usado para suspender (C09): apagar passa a
    // equivaler a desativar, preservando a identidade no histórico. Como
    // o registo continua a existir, o email também fica bloqueado para
    // reutilização (users.email é @Column(unique = true)).
    public void apagar(Long id) {
        User user = buscarEntidade(id);
        user.setStatus("INATIVO");
        userRepository.save(user);
    }

    private User buscarEntidade(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado: " + id));
    }

    private UserResponseDTO converterParaDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getNif(),
                user.getStatus(),
                user.getPapel(),
                user.getEmpresaId()
        );
    }
}
