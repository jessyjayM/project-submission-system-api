package mz.co.isctem.tcc.workflow;

import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.comum.excepcao.AcessoNegadoException;
import mz.co.isctem.tcc.dominio.modelo.Tcc;
import mz.co.isctem.tcc.dominio.repositorio.DocenteRepositorio;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import mz.co.isctem.tcc.seguranca.UtilizadorAutenticado;
import org.springframework.stereotype.Component;

/**
 * RNF06: isolamento de dados por perfil.
 *
 * <p>Traduz para codigo o quadro de permissoes da seccao 5.5 e as regras de negocio finais:
 *
 * <ul>
 *   <li>o estudante apenas visualiza o seu proprio processo;
 *   <li>o supervisor apenas visualiza os estudantes sob a sua orientacao;
 *   <li>o Coordenador tem acesso a todos os processos;
 *   <li>o Administrador gere contas, nao processos de TCC.
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PoliticaAcessoTcc {

    private final ContextoSeguranca contexto;
    private final DocenteRepositorio docentes;

    /** Lanca {@link AcessoNegadoException} se o utilizador corrente nao puder consultar este TCC. */
    public UtilizadorAutenticado exigirLeitura(Tcc tcc) {
        UtilizadorAutenticado actual = contexto.actual();
        if (podeLer(tcc, actual)) {
            return actual;
        }
        throw new AcessoNegadoException("Nao tem permissao para consultar este processo de TCC.");
    }

    /** Apenas o estudante dono do processo pode submeter documentos. */
    public UtilizadorAutenticado exigirEstudanteDono(Tcc tcc) {
        UtilizadorAutenticado actual = contexto.actual();
        if (actual.estudante()
                && tcc.getEstudante().getUtilizador().getId().equals(actual.id())) {
            return actual;
        }
        throw new AcessoNegadoException("Apenas o estudante titular do processo pode submeter documentos.");
    }

    /** Apenas o supervisor actualmente atribuido pode aprovar ou devolver submissoes. */
    public UtilizadorAutenticado exigirSupervisorDoTcc(Tcc tcc) {
        UtilizadorAutenticado actual = contexto.actual();
        if (actual.docente() && supervisorDo(tcc, actual)) {
            return actual;
        }
        throw new AcessoNegadoException(
                "Apenas o supervisor atribuido a este estudante pode avaliar as submissoes.");
    }

    public boolean podeLer(Tcc tcc, UtilizadorAutenticado actual) {
        if (actual.coordenador()) {
            return true;
        }
        if (actual.estudante()) {
            return tcc.getEstudante().getUtilizador().getId().equals(actual.id());
        }
        if (actual.docente()) {
            return supervisorDo(tcc, actual);
        }
        return false;
    }

    private boolean supervisorDo(Tcc tcc, UtilizadorAutenticado actual) {
        if (!tcc.getEstudante().temSupervisor()) {
            return false;
        }
        return docentes
                .findByUtilizadorId(actual.id())
                .map(docente -> docente.getId().equals(tcc.getEstudante().getSupervisor().getId()))
                .orElse(false);
    }
}
