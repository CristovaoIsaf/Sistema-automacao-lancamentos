package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.LoginRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LoginResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.RegistoRequestDTO;
import isaf.tfc.autolancamentosbackend.model.Empresa;
import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.EmpresaRepository;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import isaf.tfc.autolancamentosbackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Senha inválida");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getPapel());

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                "Bearer",
                user.getEmail(),
                user.getNome(),
                user.getPapel().name()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Fase 20 do plano de 20 fases — "registo público". Só disponível
     * enquanto NÃO existir nenhum utilizador nesta instalação (bootstrap
     * do primeiro Administrador) — depois disso, criar utilizadores volta
     * a ser um acto exclusivo do Administrador via POST /api/utilizadores
     * (RN006, ver UserController). Nunca cria uma segunda Empresa: se já
     * existir uma (semeada manualmente ou por um registo anterior), o novo
     * Administrador é anexado a essa mesma empresa única (ver
     * EmpresaService — "uma única empresa por instalação").
     */
    @PostMapping("/registo")
    public ResponseEntity<LoginResponseDTO> registar(@RequestBody RegistoRequestDTO dto) {
        if (userRepository.count() > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Empresa empresa = empresaRepository.findAll().stream().findFirst().orElseGet(() -> {
            Empresa nova = new Empresa();
            nova.setNome(dto.getNomeEmpresa());
            nova.setNif(dto.getNifEmpresa());
            nova.setEstado("ATIVO");
            return empresaRepository.save(nova);
        });

        User user = new User();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setNif(dto.getNif());
        user.setStatus("ATIVO");
        user.setSenha(passwordEncoder.encode(dto.getSenha()));
        user.setPapel(Role.ADMINISTRADOR);
        user.setEmpresaId(empresa.getId());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getPapel());

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                "Bearer",
                user.getEmail(),
                user.getNome(),
                user.getPapel().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}