package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.UserRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.UserResponseDTO;
import isaf.tfc.autolancamentosbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Fase 1 do plano de 20 fases — gestão de utilizadores é um acto
// administrativo (Fase 15: "administrador deve poder gerir utilizadores").
// Antes desta fase, este controller não tinha nenhuma restrição de papel —
// qualquer utilizador autenticado, incluindo Auditor, conseguia criar,
// editar ou apagar contas de outros utilizadores.
@RestController
@RequestMapping("/api/utilizadores")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Leitura: Administrador gere, Auditor consulta (Fase 16 — visão de
    // auditoria sobre utilizadores). Contabilista não precisa de ver a
    // lista de contas de outros utilizadores.
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITOR')")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> listar() {
        return ResponseEntity.ok(userService.listar());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITOR')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(userService.buscarPorId(id));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<UserResponseDTO> criar(@RequestBody UserRequestDTO dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.criar(dados));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> atualizar(@PathVariable Long id, @RequestBody UserRequestDTO dados) {
        return ResponseEntity.ok(userService.atualizar(id, dados));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        userService.apagar(id);
        return ResponseEntity.noContent().build();
    }
}
