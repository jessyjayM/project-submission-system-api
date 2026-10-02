package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mz.co.isctem.tcc.dominio.enums.AreaCurso;
import mz.co.isctem.tcc.dominio.enums.EstadoTcc;
import mz.co.isctem.tcc.dominio.enums.FaseTcc;

/** Processo de Trabalho de Conclusao de Curso de um estudante (RF05). */
@Entity
@Table(name = "tcc")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tcc extends EntidadeAuditada {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estudante_id", nullable = false, unique = true)
    private Estudante estudante;

    /** Preenchido quando o tema e submetido e aprovado na Fase 1. */
    @Column(length = 300)
    private String titulo;

    @Column(nullable = false, length = 120)
    private String curso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AreaCurso area;

    @Column(name = "data_inicio", nullable = false)
    @Builder.Default
    private LocalDate dataInicio = LocalDate.now();

    @Column(name = "data_fim_prevista")
    private LocalDate dataFimPrevista;

    @Column(name = "fase_actual", nullable = false)
    @Builder.Default
    private int faseActual = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_geral", nullable = false, length = 30)
    @Builder.Default
    private EstadoTcc estadoGeral = EstadoTcc.EM_CURSO;

    @OneToMany(mappedBy = "tcc", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    @Builder.Default
    private List<Fase> fases = new ArrayList<>();

    public Optional<Fase> fase(int numero) {
        return fases.stream().filter(f -> f.getNumero() == numero).findFirst();
    }

    public Optional<Fase> faseCorrente() {
        return fase(faseActual);
    }

    /** Numero da ultima fase aplicavel: 4 para cursos gerais, 5 para Informatica. */
    public int ultimaFase() {
        return FaseTcc.ultimaFase(area);
    }

    /** RF11: percentagem de conclusao apresentada ao estudante. */
    public int percentagemConclusao() {
        if (fases.isEmpty()) {
            return 0;
        }
        long aprovadas = fases.stream().filter(f -> f.getEstado().terminal()).count();
        return (int) Math.round(aprovadas * 100.0 / fases.size());
    }
}
