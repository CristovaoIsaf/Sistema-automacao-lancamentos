package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.UserRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.UserResponseDTO;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.service.AuditLogService;
import isaf.tfc.autolancamentosbackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    // Auditoria C06/C07 — gestão de utilizadores nunca tinha nenhum
    // registo de auditoria (ver AuditLog e AuditoriaService.eventosDeAudit
    // Log, que passou a incluir estes eventos).
    private final AuditLogService auditLogService;

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
    public ResponseEntity<UserResponseDTO> criar(
            @RequestBody UserRequestDTO dados,
            @AuthenticationPrincipal User admin,
            HttpServletRequest httpRequest
    ) {
        UserResponseDTO criado = userService.criar(dados);
        auditLogService.registar(admin, "CRIAR_UTILIZADOR", "User", criado.getId(),
                AuditLogService.SUCESSO, "papel=" + criado.getPapel(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody UserRequestDTO dados,
            @AuthenticationPrincipal User admin,
            HttpServletRequest httpRequest
    ) {
        UserResponseDTO atualizado = userService.atualizar(id, dados);
        auditLogService.registar(admin, "ATUALIZAR_UTILIZADOR", "User", id,
                AuditLogService.SUCESSO, "papel=" + atualizado.getPapel() + ", status=" + atualizado.getStatus(),
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(atualizado);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id, @AuthenticationPrincipal User admin, HttpServletRequest httpRequest) {
        userService.apagar(id);
        // Auditoria C08 — "apagar" é soft-delete (status=INATIVO), ver
        // UserService.apagar; o log deixa claro que foi um pedido de
        // remoção, não só mais uma atualização de perfil.
        auditLogService.registar(admin, "APAGAR_UTILIZADOR", "User", id,
                AuditLogService.SUCESSO, null, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
