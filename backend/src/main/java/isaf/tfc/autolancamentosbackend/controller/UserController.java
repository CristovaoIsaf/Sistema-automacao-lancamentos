package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.UserRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.UserResponseDTO;
import isaf.tfc.autolancamentosbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilizadores")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> listar() {
        return ResponseEntity.ok(userService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(userService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> criar(@RequestBody UserRequestDTO dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.criar(dados));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> atualizar(@PathVariable Long id, @RequestBody UserRequestDTO dados) {
        return ResponseEntity.ok(userService.atualizar(id, dados));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {
        userService.apagar(id);
        return ResponseEntity.noContent().build();
    }
}
