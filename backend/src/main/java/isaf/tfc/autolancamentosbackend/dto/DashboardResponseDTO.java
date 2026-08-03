package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponseDTO {

    private KpiDashboardDTO kpis;

    private List<DadoGraficoMensalDTO> graficoMensal;

    private List<DocumentoRecenteDTO> documentosRecentes;
}
