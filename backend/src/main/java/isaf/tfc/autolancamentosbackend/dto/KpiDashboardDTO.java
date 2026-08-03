package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KpiDashboardDTO {

    private long documentosImportados;

    private long sugestoesPendentes;

    private long lancamentosAprovados;

    private double precisaoIA;
}
