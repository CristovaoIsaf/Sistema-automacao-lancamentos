package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos")
@Data
public class DocumentoContabilistico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeFicheiro;

    // Ex: "application/pdf", "image/jpeg", "image/png"
    private String tipoConteudo;

    @Lob
    private byte[] conteudo;

    private LocalDateTime dataUpload;

    // SHA-256 (hex) dos bytes do ficheiro — aditivo, usado para impedir
    // que o mesmo documento seja carregado duas vezes (ver
    // DocumentoController.upload). Documentos antigos ficam com null.
    private String hashConteudo;

    // id do User que fez o upload; sem relação JPA para manter o padrão
    // já usado em Lancamento (sugestaoId, validadoPor)
    private Long userId;

    // Aditivo (T2 — arquivo por entidade), preenchido em analisarDocumento
    // a partir do NIF/entidade que a análise devolve. Fica null até o
    // documento ser analisado.
    private Long entidadeId;

    @jakarta.persistence.PrePersist
    public void aoCriar() {
        this.dataUpload = LocalDateTime.now();
    }
}
