package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    // Fase 2 — contexto contabilístico da empresa (ver nota FACTO/CONTEXTO
    // em model/Empresa.java). Nunca preenchido pela IA — só por um
    // Administrador, em /configuracoes.
    private String atividadeEconomica;

    private String naturezaNegocio;

    private String moeda;

    private LocalDate exercicioAtualInicio;

    private LocalDate exercicioAtualFim;
}
