package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mz.co.isctem.tcc.dominio.enums.EstadoSubmissao;

/**
 * Submissao do estudante numa fase (RF05, RF06).
 *
 * <p>Cada submissao gera automaticamente uma nova versao do documento; as versoes anteriores
 * permanecem armazenadas.
 */
@Entity
@Table(name = "submissao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submissao extends EntidadeAuditada {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fase_id", nullable = false)
    private Fase fase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Utilizador autor;

    /** Ordem da submissao dentro da fase (1, 2, 3...). */
    @Column(nullable = false)
    private int numero;

    @Column(name = "comentario_estudante", length = 2000)
    private String comentarioEstudante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoSubmissao estado = EstadoSubmissao.EM_REVISAO;

    @Column(name = "data_submissao", nullable = false)
    @Builder.Default
    private Instant dataSubmissao = Instant.now();

    /** Momento em que o supervisor decidiu; base do tempo medio de resposta (RF11). */
    @Column(name = "data_decisao")
    private Instant dataDecisao;

    @OneToMany(mappedBy = "submissao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroVersao ASC")
    @Builder.Default
    private List<VersaoDocumento> versoes = new ArrayList<>();

    @OneToMany(mappedBy = "submissao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataRevisao ASC")
    @Builder.Default
    private List<Revisao> revisoes = new ArrayList<>();

    public Optional<VersaoDocumento> versaoMaisRecente() {
        return versoes.stream().max((a, b) -> Integer.compare(a.getNumeroVersao(), b.getNumeroVersao()));
    }
}
