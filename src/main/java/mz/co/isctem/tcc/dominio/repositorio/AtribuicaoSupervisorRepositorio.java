package mz.co.isctem.tcc.dominio.repositorio;

import java.util.List;
import java.util.Optional;
import mz.co.isctem.tcc.dominio.modelo.AtribuicaoSupervisor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtribuicaoSupervisorRepositorio extends JpaRepository<AtribuicaoSupervisor, Long> {

    /** Atribuicao actualmente em vigor (RF04: no maximo uma por estudante). */
    Optional<AtribuicaoSupervisor> findByEstudanteIdAndFimIsNull(Long estudanteId);

    List<AtribuicaoSupervisor> findByEstudanteIdOrderByInicioDesc(Long estudanteId);
}
