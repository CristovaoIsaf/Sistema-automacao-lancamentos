package isaf.tfc.autolancamentosbackend.dto;

public class AnalisarDocumentoRequest {

    private Long documentoId;

    public AnalisarDocumentoRequest() {
    }

    public AnalisarDocumentoRequest(Long documentoId) {
        this.documentoId = documentoId;
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
    }
}