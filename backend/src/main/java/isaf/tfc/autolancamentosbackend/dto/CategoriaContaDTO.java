package isaf.tfc.autolancamentosbackend.dto;

import java.util.List;

/**
 * Camada de categorização sobre o plano de contas real (ContaController.CONTAS)
 * — não define contas novas, só agrupa as já existentes por tipo de operação
 * (Vendas, Compras, ...), distinguindo contas principais das ocasionais.
 * Uma mesma conta pode aparecer em várias categorias.
 */
public record CategoriaContaDTO(String nome, List<ContaDTO> principais, List<ContaDTO> ocasionais) {
}
