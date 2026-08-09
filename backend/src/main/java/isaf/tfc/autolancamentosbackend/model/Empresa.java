package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "empresas")
@Data
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String nif;

    private String email;

    private String endereco;

    private String telefone;

    private String estado;

    // Fase 2 do plano de 20 fases — "contexto contabilístico da empresa".
    // Aditivos, todos nullable (ficam por preencher até um Administrador os
    // configurar em /configuracoes — nunca inferidos pela IA, ver regra
    // explícita da Fase 2: "o contexto NÃO deve ser inventado pela IA").
    //
    // Distinção FACTO vs CONTEXTO pedida pela Fase 2 (documentada aqui e
    // reflectida na UI — ver Configuracoes.tsx, secção "Geral"):
    //   FACTO    — dado objectivo, sem ambiguidade (moeda, datas do exercício).
    //   CONTEXTO — descrição qualitativa do negócio, usada para desambiguar
    //              classificações futuras (Fase 5) — ex: "viatura" significa
    //              mercadoria para um stand automóvel, mas ativo para uma
    //              construtora.

    // CONTEXTO — ex: "Comércio a retalho de veículos automóveis".
    private String atividadeEconomica;

    // CONTEXTO — ex: "Revenda de mercadorias" vs "Prestação de serviços".
    private String naturezaNegocio;

    // FACTO
    private String moeda;

    // FACTO — início/fim do exercício contabilístico corrente. Usado pelas
    // fases seguintes (ex: Balancete) que hoje não têm noção nenhuma de
    // período/exercício fechado.
    private LocalDate exercicioAtualInicio;

    private LocalDate exercicioAtualFim;
}
