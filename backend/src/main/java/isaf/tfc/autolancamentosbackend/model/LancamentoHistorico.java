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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Auditoria C04: até esta correção, editar um Lancamento (ver
 * LancamentoServiceImpl.atualizar) substituía data/descrição/linhas
 * diretamente — os valores ANTERIORES à edição eram perdidos, sem
 * nenhuma forma de reconstruir "o que estava lá antes". Uma linha por
 * cada edição feita a um Lancamento: o snapshot de como ele estava
 * IMEDIATAMENTE ANTES desta alteração ser aplicada — nunca alterado
 * depois de gravado (é histórico).
 */
@Entity
@Table(name = "lancamento_historico")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LancamentoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lancamento_id", nullable = false)
    private Long lancamentoId;

    @Column(name = "data_anterior")
    private LocalDate dataAnterior;

    @Column(name = "descricao_anterior", columnDefinition = "TEXT")
    private String descricaoAnterior;

    // Serializado tal como Sugestao.linhasJson (mesmo padrão já usado no
    // projeto para guardar uma lista de LinhaLancamentoDTO como texto).
    @Column(name = "linhas_anteriores_json", columnDefinition = "TEXT")
    private String linhasAnterioresJson;

    @Column(name = "alterado_por")
    private Long alteradoPor;

    @Column(name = "alterado_em")
    private LocalDateTime alteradoEm;

    @PrePersist
    public void aoCriar() {
        if (this.alteradoEm == null) {
            this.alteradoEm = LocalDateTime.now();
        }
    }
}
