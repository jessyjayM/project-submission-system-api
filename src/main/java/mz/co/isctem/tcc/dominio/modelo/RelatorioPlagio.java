package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mz.co.isctem.tcc.dominio.enums.EstadoRelatorioPlagio;

/** Relatorio de similaridade gerado automaticamente a cada submissao (RF08). */
@Entity
@Table(name = "relatorio_plagio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioPlagio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_documento_id", nullable = false, unique = true)
    private VersaoDocumento versaoDocumento;

    @Column(name = "percentagem_similaridade", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal percentagemSimilaridade = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoRelatorioPlagio estado = EstadoRelatorioPlagio.CONCLUIDO;

    @Column(name = "documentos_comparados", nullable = false)
    @Builder.Default
    private int documentosComparados = 0;

    @Column(length = 500)
    private String mensagem;

    @Column(name = "data_geracao", nullable = false)
    @Builder.Default
    private Instant dataGeracao = Instant.now();

    @OneToMany(mappedBy = "relatorio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("percentagemSimilaridade DESC")
    @Builder.Default
    private List<PlagioCorrespondencia> correspondencias = new ArrayList<>();
}
