package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mz.co.isctem.tcc.dominio.enums.DecisaoRevisao;

/** Avaliacao do supervisor sobre uma submissao (RF05, RNF04). */
@Entity
@Table(name = "revisao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Revisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submissao_id", nullable = false)
    private Submissao submissao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revisor_id", nullable = false)
    private Utilizador revisor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DecisaoRevisao decisao;

    @Column(length = 4000)
    private String comentario;

    @Column(name = "data_revisao", nullable = false)
    @Builder.Default
    private Instant dataRevisao = Instant.now();
}
