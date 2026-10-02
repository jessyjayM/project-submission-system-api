package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Documento semelhante encontrado no corpus interno durante a analise (RF08). */
@Entity
@Table(name = "plagio_correspondencia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlagioCorrespondencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relatorio_id", nullable = false)
    private RelatorioPlagio relatorio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_semelhante_id", nullable = false)
    private VersaoDocumento versaoSemelhante;

    @Column(name = "percentagem_similaridade", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentagemSimilaridade;

    /** Identificacao legivel do documento semelhante (estudante, fase e versao). */
    @Column(nullable = false, length = 300)
    private String descricao;
}
