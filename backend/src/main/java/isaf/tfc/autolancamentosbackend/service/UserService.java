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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        return converterParaDTO(userRepository.save(user));
    }

    public void apagar(Long id) {
        userRepository.delete(buscarEntidade(id));
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
                user.getPapel()
        );
    }
}
