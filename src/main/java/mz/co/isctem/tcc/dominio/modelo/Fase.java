package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mz.co.isctem.tcc.dominio.enums.EstadoFase;

/**
 * Fase do workflow de um TCC (seccao 5.4).
 *
 * <p>{@code emAtraso} e um marcador transversal: acumula com o estado do fluxo em vez de o
 * substituir, permitindo apresentar combinacoes como "Em Revisao - Em Atraso".
 */
@Entity
@Table(name = "fase")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fase extends EntidadeAuditada {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tcc_id", nullable = false)
    private Tcc tcc;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoFase estado = EstadoFase.PENDENTE;

    @Column(name = "em_atraso", nullable = false)
    @Builder.Default
    private boolean emAtraso = false;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_limite")
    private LocalDate dataLimite;

    @Column(name = "data_aprovacao")
    private Instant dataAprovacao;

    /** Dias que faltam ate a data limite; negativo quando ja foi ultrapassada. */
    public Long diasRestantes() {
        if (dataLimite == null || estado.terminal()) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), dataLimite);
    }

    /** Dias decorridos para la da data limite, ou 0 se ainda estiver dentro do prazo. */
    public long diasEmAtraso() {
        Long restantes = diasRestantes();
        return restantes == null || restantes >= 0 ? 0 : -restantes;
    }

    /** RF10: o prazo foi ultrapassado sem que a fase tivesse sido aprovada. */
    public boolean prazoUltrapassado(LocalDate referencia) {
        return dataLimite != null && !estado.terminal() && referencia.isAfter(dataLimite);
    }

    /** Rotulo apresentado ao utilizador, combinando o estado do fluxo com o atraso. */
    public String estadoApresentacao() {
        String base =
                switch (estado) {
                    case PENDENTE -> "Pendente";
                    case EM_REVISAO -> "Em Revisao";
                    case CORRECAO_SOLICITADA -> "Correcao Solicitada";
                    case APROVADO -> "Aprovado";
                };
        return emAtraso ? base + " - Em Atraso" : base;
    }
}
