package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fase 15 do plano de 20 fases — "consultar auditoria": um evento do
 * registo de auditoria (ver AuditoriaService). Espelha o tipo LogAuditoria
 * já existente no frontend (types/contabilidade.ts) — antes desta fase
 * preenchido só com dados de exemplo estáticos (`logsMock` em
 * Auditoria.tsx), sem nenhum backend real por trás.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogAuditoriaDTO {

    private String id;

    private String utilizador;

    private String perfil;

    private String acao;

    private String entidade;

    private LocalDateTime dataHora;

    // Auditoria C06/C07 — só preenchidos para eventos vindos da tabela
    // AuditLog (ver AuditoriaService.eventosDeAuditLog); os eventos
    // derivados de Lancamento/Documento continuam sem "resultado" próprio
    // (são sempre uma operação que teve sucesso — se tivesse falhado, não
    // haveria linha nenhuma para derivar).
    private String resultado;

    private String motivo;

    private String ip;
}
