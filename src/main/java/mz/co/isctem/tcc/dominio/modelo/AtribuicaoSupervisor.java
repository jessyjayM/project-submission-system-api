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
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Historico de atribuicoes de supervisor (RF04).
 *
 * <p>A atribuicao activa e a que tem {@code fim == null}; um indice unico parcial na base de dados
 * garante que existe no maximo uma por estudante.
 */
@Entity
@Table(name = "atribuicao_supervisor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtribuicaoSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudante_id", nullable = false)
    private Estudante estudante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atribuido_por", nullable = false)
    private Utilizador atribuidoPor;

    @Column(nullable = false)
    @Builder.Default
    private Instant inicio = Instant.now();

    private Instant fim;

    @Column(length = 500)
    private String motivo;
}
