package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.TwoFactorCodigoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.TwoFactorConfirmResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.TwoFactorDesativarRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.TwoFactorSetupResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.TwoFactorStatusResponseDTO;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.service.TwoFactorAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Autenticação de dois factores — sempre sobre o utilizador autenticado
 * (nunca recebe nem aceita um id de outro utilizador como alvo). Qualquer
 * papel pode ativar a própria: proteger a conta não é um privilégio de
 * Administrador, é algo que ADMINISTRADOR/CONTABILISTA/AUDITOR fazem cada
 * um pela sua própria conta — por isso nenhum destes endpoints tem
 * @PreAuthorize além de "autenticado" (herdado de SecurityConfig).
 */
@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorAuthService twoFactorAuthService;

    @GetMapping("/status")
    public ResponseEntity<TwoFactorStatusResponseDTO> status(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new TwoFactorStatusResponseDTO(user.isTwoFactorEnabled()));
    }

    @PostMapping("/setup")
    public ResponseEntity<TwoFactorSetupResponseDTO> setup(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(twoFactorAuthService.iniciarSetup(user));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<TwoFactorConfirmResponseDTO> confirmar(
            @AuthenticationPrincipal User user,
            @RequestBody TwoFactorCodigoRequestDTO request
    ) {
        return ResponseEntity.ok(new TwoFactorConfirmResponseDTO(
                twoFactorAuthService.confirmarSetup(user, request.getCodigo())));
    }

    @PostMapping("/desativar")
    public ResponseEntity<Void> desativar(
            @AuthenticationPrincipal User user,
            @RequestBody TwoFactorDesativarRequestDTO request
    ) {
        twoFactorAuthService.desativar(user, request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
