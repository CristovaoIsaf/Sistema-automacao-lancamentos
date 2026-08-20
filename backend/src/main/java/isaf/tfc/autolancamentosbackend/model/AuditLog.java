package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Auditoria C06/C07: até esta correção não existia nenhuma tabela de audit
 * log — o que a página de Auditoria mostrava era inteiramente DERIVADO de
 * colunas de timestamp já existentes em Lancamento/DocumentoContabilistico
 * (ver AuditoriaService), cobrindo só 2 tipos de evento. Esta tabela é
 * apensa (nenhum UPDATE/DELETE em lado nenhum do código escreve nela depois
 * do INSERT inicial) e cobre especificamente os pontos que ficavam de fora:
 * gestão de utilizadores, alteração da empresa, rejeição de sugestões da
 * IA, e tentativas de login (sucesso e falha). Os eventos derivados de
 * Lancamento/Documento continuam a existir em paralelo (ver
 * AuditoriaService.listarTodosOsEventos) — não foram todos migrados para
 * esta tabela nesta correção, para manter o âmbito controlado.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sem FK de propósito (mesmo padrão do resto do projeto — ver
    // Lancamento.validadoPor) — mas nome/perfil ficam sempre gravados aqui
    // como snapshot no momento da ação (colunas abaixo), por isso, ao
    // contrário do resto do sistema, apagar/alterar o utilizador depois
    // NUNCA apaga a identidade que already está neste registo.
    @Column(name = "utilizador_id")
    private Long utilizadorId;

    @Column(name = "utilizador_nome")
    private String utilizadorNome;

    private String papel;

    @Column(nullable = false)
    private String acao;

    private String entidade;

    @Column(name = "entidade_id")
    private Long entidadeId;

    // "SUCESSO" | "FALHA"
    @Column(nullable = false)
    private String resultado;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    private String ip;

    private LocalDateTime timestamp;

    @PrePersist
    public void aoCriar() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
