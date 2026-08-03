package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmpresaDTO {

    private Long id;

    private String nome;

    private String nif;

    private String email;

    private String endereco;

    private String telefone;
}
