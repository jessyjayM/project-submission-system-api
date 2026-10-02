package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Marco/prazo definido pelo Coordenador para uma fase de um curso num ano lectivo (RF10).
 *
 * <p>Funciona como modelo: ao arrancar o workflow de um estudante as datas sao copiadas para as
 * respectivas fases, podendo depois ser ajustadas caso a caso.
 */
@Entity
@Table(name = "milestone")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Milestone extends EntidadeAuditada {

    @Column(nullable = false, length = 120)
    private String curso;

    @Column(name = "ano_lectivo", nullable = false)
    private int anoLectivo;

    @Column(name = "numero_fase", nullable = false)
    private int numeroFase;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_limite", nullable = false)
    private LocalDate dataLimite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por")
    private Utilizador criadoPor;
}
