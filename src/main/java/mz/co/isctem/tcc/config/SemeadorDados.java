package mz.co.isctem.tcc.config;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.dominio.enums.AreaCurso;
import mz.co.isctem.tcc.dominio.enums.FaseTcc;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;
import mz.co.isctem.tcc.dominio.modelo.Docente;
import mz.co.isctem.tcc.dominio.modelo.Estudante;
import mz.co.isctem.tcc.dominio.modelo.Milestone;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.DocenteRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.EstudanteRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.MilestoneRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados minimos de arranque.
 *
 * <p>Cria a conta de Administrador quando ainda nao existe nenhuma — sem ela nao seria possivel
 * criar as restantes contas, ja que RF01 reserva essa operacao ao Administrador. As contas de
 * demonstracao ficam atras de {@code isctem.seed.demo} e devem ser desligadas em producao.
 *
 * <p>Todas as contas criadas aqui nascem com {@code deveAlterarSenha = true}, como manda o RF03.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemeadorDados implements ApplicationRunner {

    private final UtilizadorRepositorio utilizadores;
    private final DocenteRepositorio docentes;
    private final EstudanteRepositorio estudantes;
    private final MilestoneRepositorio milestones;
    private final PasswordEncoder codificador;
    private final PropriedadesIsctem propriedades;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        PropriedadesIsctem.Seed seed = propriedades.seed();
        if (seed == null) {
            return;
        }
        if (seed.adminInicial()) {
            criarAdministradorInicial(seed);
        }
        if (seed.demo()) {
            criarDadosDemonstracao();
        }
    }

    private void criarAdministradorInicial(PropriedadesIsctem.Seed seed) {
        if (!utilizadores.findByTipo(PerfilUtilizador.ADMIN).isEmpty()) {
            return;
        }
        utilizadores.save(
                Utilizador.builder()
                        .nome("Administrador do Sistema")
                        .tipo(PerfilUtilizador.ADMIN)
                        .email(seed.adminEmail())
                        .senhaHash(codificador.encode(seed.adminSenha()))
                        .deveAlterarSenha(true)
                        .build());
        log.info(
                "Conta de Administrador criada: {} (a senha inicial tem de ser alterada no primeiro acesso).",
                seed.adminEmail());
    }

    private void criarDadosDemonstracao() {
        if (utilizadores.count() > 1) {
            return;
        }
        log.info("A criar dados de demonstracao (isctem.seed.demo=true).");

        criarUtilizador("Coordenacao de TCC", PerfilUtilizador.COORDENADOR, "coordenador@isctem.co.mz", null);

        Utilizador docenteUm =
                criarUtilizador("Ana Machava", PerfilUtilizador.DOCENTE, "ana.machava@isctem.co.mz", null);
        Utilizador docenteDois =
                criarUtilizador("Carlos Nhampossa", PerfilUtilizador.DOCENTE, "carlos.nhampossa@isctem.co.mz", null);
        docentes.save(Docente.builder().utilizador(docenteUm).departamento("Engenharia Informatica").build());
        docentes.save(Docente.builder().utilizador(docenteDois).departamento("Gestao").build());

        criarEstudante("Julia Sitoe", "20210145", "Engenharia Informatica", AreaCurso.INFORMATICA);
        criarEstudante("Nelson Cuna", "20210233", "Engenharia Informatica", AreaCurso.INFORMATICA);
        criarEstudante("Rita Bila", "20210398", "Gestao de Empresas", AreaCurso.OUTRA);

        criarPlanoDePrazos("Engenharia Informatica", AreaCurso.INFORMATICA);
        criarPlanoDePrazos("Gestao de Empresas", AreaCurso.OUTRA);
    }

    private Utilizador criarUtilizador(
            String nome, PerfilUtilizador tipo, String email, String codigo) {
        return utilizadores.save(
                Utilizador.builder()
                        .nome(nome)
                        .tipo(tipo)
                        .email(email)
                        .codigo(codigo)
                        .senhaHash(codificador.encode(propriedades.seed().adminSenha()))
                        .deveAlterarSenha(true)
                        .build());
    }

    private void criarEstudante(String nome, String codigo, String curso, AreaCurso area) {
        Utilizador utilizador = criarUtilizador(nome, PerfilUtilizador.ESTUDANTE, null, codigo);
        estudantes.save(Estudante.builder().utilizador(utilizador).curso(curso).area(area).build());
    }

    /** Plano de prazos com cerca de dois meses por fase, a partir do inicio do ano lectivo. */
    private void criarPlanoDePrazos(String curso, AreaCurso area) {
        int ano = LocalDate.now().getYear();
        LocalDate inicio = LocalDate.of(ano, 2, 1);
        List<FaseTcc> aplicaveis = FaseTcc.aplicaveis(area);

        for (FaseTcc fase : aplicaveis) {
            LocalDate limite = inicio.plusDays(59);
            milestones.save(
                    Milestone.builder()
                            .curso(curso)
                            .anoLectivo(ano)
                            .numeroFase(fase.numero())
                            .nome(fase.nomeApresentacao())
                            .dataInicio(inicio)
                            .dataLimite(limite)
                            .build());
            inicio = limite.plusDays(1);
        }
    }
}
