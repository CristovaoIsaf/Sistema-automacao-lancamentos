package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Versão de DocumentoContabilistico sem o campo "conteudo" (byte[]),
 * para não devolver o ficheiro inteiro em cada item de uma listagem.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoResponseDTO {

    private Long id;

    private String nomeFicheiro;

    private String tipoConteudo;

    private LocalDateTime dataUpload;

    // Campos aditivos — arquivo organizado (ver DocumentoController.listar()).
    private Long entidadeId;

    private String entidadeNome;

    private long tamanho;

    // "Pendente" | "Analisado" | "Aprovado" | "Rejeitado" — derivado da Sugestao
    // mais recente ligada a este documento (ver DocumentoController.listar()).
    private String estado;
}
