package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fase 15 do plano de 20 fases — "acompanhar contabilistas": resumo de
 * atividade de um utilizador (ver AuditoriaService.resumoPorUtilizador),
 * derivado dos mesmos eventos usados no registo de auditoria.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AtividadeUtilizadorDTO {

    private String utilizador;

    private String perfil;

    private long totalAcoes;

    private LocalDateTime ultimaAcao;
}
