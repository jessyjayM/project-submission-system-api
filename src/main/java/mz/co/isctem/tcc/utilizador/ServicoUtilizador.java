package mz.co.isctem.tcc.utilizador;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.comum.GeradorSenha;
import mz.co.isctem.tcc.comum.PaginaResposta;
import mz.co.isctem.tcc.comum.ServicoAuditoria;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.comum.excepcao.RegraNegocioException;
import mz.co.isctem.tcc.dominio.enums.EstadoProcessoEstudante;
import mz.co.isctem.tcc.dominio.enums.EstadoUtilizador;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;
import mz.co.isctem.tcc.dominio.enums.TipoNotificacao;
import mz.co.isctem.tcc.dominio.modelo.Docente;
import mz.co.isctem.tcc.dominio.modelo.Estudante;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.DocenteRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.EstudanteRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import mz.co.isctem.tcc.notificacao.ServicoNotificacao;
import mz.co.isctem.tcc.seguranca.ContextoSeguranca;
import mz.co.isctem.tcc.seguranca.ServicoTokens;
import mz.co.isctem.tcc.utilizador.dto.ActualizarUtilizadorPedido;
import mz.co.isctem.tcc.utilizador.dto.ContaCriadaResposta;
import mz.co.isctem.tcc.utilizador.dto.CriarUtilizadorPedido;
import mz.co.isctem.tcc.utilizador.dto.UtilizadorResposta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF01: gestao de utilizadores.
 *
 * <p>Todas as operacoes desta classe sao exclusivas do Administrador (quadro de permissoes da
 * seccao 5.5); a restricao por perfil e imposta na cadeia de filtros do Spring Security.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoUtilizador {

    private final UtilizadorRepositorio utilizadores;
    private final EstudanteRepositorio estudantes;
    private final DocenteRepositorio docentes;
    private final PasswordEncoder codificador;
    private final UtilizadorMapper mapper;
    private final ServicoAuditoria auditoria;
    private final ServicoNotificacao notificacoes;
    private final ServicoTokens tokens;
    private final ContextoSeguranca contexto;

    @Transactional(readOnly = true)
    public PaginaResposta<UtilizadorResposta> listar(String termo, PerfilUtilizador tipo, Pageable pagina) {
        Page<Utilizador> resultado = utilizadores.pesquisar(vazioParaNulo(termo), tipo, pagina);
        return PaginaResposta.de(resultado, this::enriquecer);
    }

    @Transactional(readOnly = true)
    public UtilizadorResposta porId(Long id) {
        return enriquecer(carregar(id));
    }

    /**
     * Cria a conta e devolve a senha temporaria, que o utilizador tera obrigatoriamente de trocar no
     * primeiro acesso (RF03).
     */
    @Transactional
    public ContaCriadaResposta criar(CriarUtilizadorPedido pedido) {
        validarCredenciais(pedido.tipo(), pedido.email(), pedido.codigo(), null);
        validarDadosDoPerfil(pedido.tipo(), pedido.curso(), pedido.area(), pedido.departamento());

        String senhaTemporaria = GeradorSenha.gerar();
        Utilizador utilizador =
                Utilizador.builder()
                        .nome(pedido.nome().trim())
                        .tipo(pedido.tipo())
                        .email(normalizar(pedido.email()))
                        .codigo(normalizar(pedido.codigo()))
                        .senhaHash(codificador.encode(senhaTemporaria))
                        .estado(EstadoUtilizador.ACTIVO)
                        .deveAlterarSenha(true)
                        .build();
        utilizadores.save(utilizador);

        criarPerfilAssociado(utilizador, pedido.curso(), pedido.area(), pedido.departamento());

        auditoria.registar(
                actor(), "CRIAR_UTILIZADOR", "Utilizador", utilizador.getId(), "perfil=" + pedido.tipo());
        notificacoes.criar(
                utilizador,
                TipoNotificacao.CONTA_CRIADA,
                "Bem-vindo ao sistema de TCC do ISCTEM",
                "A sua conta foi criada. Por seguranca, tera de alterar a senha no primeiro acesso.",
                "/perfil/senha");

        return new ContaCriadaResposta(enriquecer(utilizador), senhaTemporaria);
    }

    @Transactional
    public UtilizadorResposta actualizar(Long id, ActualizarUtilizadorPedido pedido) {
        Utilizador utilizador = carregar(id);
        validarCredenciais(pedido.tipo(), pedido.email(), pedido.codigo(), id);
        validarDadosDoPerfil(pedido.tipo(), pedido.curso(), pedido.area(), pedido.departamento());

        PerfilUtilizador perfilAnterior = utilizador.getTipo();
        utilizador.setNome(pedido.nome().trim());
        utilizador.setTipo(pedido.tipo());
        utilizador.setEmail(normalizar(pedido.email()));
        utilizador.setCodigo(normalizar(pedido.codigo()));

        if (perfilAnterior != pedido.tipo()) {
            // A alteracao de perfil obriga a recriar o registo especifico correspondente.
            criarPerfilAssociado(utilizador, pedido.curso(), pedido.area(), pedido.departamento());
            tokens.revogarTodosDe(id);
        } else {
            actualizarPerfilAssociado(utilizador, pedido.curso(), pedido.area(), pedido.departamento());
        }

        auditoria.registar(
                actor(),
                "ACTUALIZAR_UTILIZADOR",
                "Utilizador",
                id,
                "perfil %s -> %s".formatted(perfilAnterior, pedido.tipo()));
        return enriquecer(utilizador);
    }

    /** RF01: activacao e desactivacao de contas. */
    @Transactional
    public UtilizadorResposta alterarEstado(Long id, EstadoUtilizador estado) {
        Utilizador utilizador = carregar(id);
        if (utilizador.getTipo() == PerfilUtilizador.ADMIN
                && estado == EstadoUtilizador.INACTIVO
                && contarAdministradoresActivos() <= 1) {
            throw new RegraNegocioException(
                    "Nao e possivel desactivar o unico Administrador activo do sistema.");
        }
        utilizador.setEstado(estado);
        if (estado == EstadoUtilizador.INACTIVO) {
            // Uma conta desactivada nao pode continuar a renovar sessoes.
            tokens.revogarTodosDe(id);
        }
        auditoria.registar(actor(), "ALTERAR_ESTADO_CONTA", "Utilizador", id, "estado=" + estado);
        return enriquecer(utilizador);
    }

    /**
     * RF03: unica forma de recuperacao de senha nesta versao do sistema. Nao existe recuperacao
     * self-service por email.
     */
    @Transactional
    public ContaCriadaResposta redefinirSenha(Long id) {
        Utilizador utilizador = carregar(id);
        String senhaTemporaria = GeradorSenha.gerar();
        utilizador.setSenhaHash(codificador.encode(senhaTemporaria));
        utilizador.setDeveAlterarSenha(true);
        utilizador.setTentativasFalhadas(0);
        utilizador.setBloqueadoAte(null);
        tokens.revogarTodosDe(id);

        auditoria.registar(actor(), "REDEFINIR_SENHA", "Utilizador", id, null);
        notificacoes.criar(
                utilizador,
                TipoNotificacao.SENHA_REDEFINIDA,
                "A sua senha foi redefinida",
                "O Administrador redefiniu a sua senha. Tera de a alterar no proximo acesso.",
                "/perfil/senha");
        return new ContaCriadaResposta(enriquecer(utilizador), senhaTemporaria);
    }

    private void criarPerfilAssociado(
            Utilizador utilizador, String curso, mz.co.isctem.tcc.dominio.enums.AreaCurso area, String departamento) {
        switch (utilizador.getTipo()) {
            case ESTUDANTE ->
                    estudantes
                            .findByUtilizadorId(utilizador.getId())
                            .ifPresentOrElse(
                                    existente -> {
                                        existente.setCurso(curso);
                                        existente.setArea(area);
                                    },
                                    () ->
                                            estudantes.save(
                                                    Estudante.builder()
                                                            .utilizador(utilizador)
                                                            .curso(curso)
                                                            .area(area)
                                                            .estadoProcesso(EstadoProcessoEstudante.SEM_SUPERVISOR)
                                                            .build()));
            case DOCENTE ->
                    docentes
                            .findByUtilizadorId(utilizador.getId())
                            .ifPresentOrElse(
                                    existente -> existente.setDepartamento(departamento),
                                    () ->
                                            docentes.save(
                                                    Docente.builder()
                                                            .utilizador(utilizador)
                                                            .departamento(departamento)
                                                            .build()));
            case ADMIN, COORDENADOR -> {
                // Nao existe entidade especifica para estes perfis.
            }
        }
    }

    private void actualizarPerfilAssociado(
            Utilizador utilizador, String curso, mz.co.isctem.tcc.dominio.enums.AreaCurso area, String departamento) {
        criarPerfilAssociado(utilizador, curso, area, departamento);
    }

    /** RF02: cada perfil tem a sua credencial de login, que tem de ser unica. */
    private void validarCredenciais(PerfilUtilizador tipo, String email, String codigo, Long idActual) {
        if (tipo.autenticaPorCodigo()) {
            if (codigo == null || codigo.isBlank()) {
                throw new RegraNegocioException("O codigo do estudante e obrigatorio para o perfil ESTUDANTE.");
            }
        } else if (email == null || email.isBlank()) {
            throw new RegraNegocioException("O email institucional e obrigatorio para o perfil " + tipo + ".");
        }

        if (email != null && !email.isBlank()) {
            utilizadores
                    .findByEmailIgnoreCase(email.trim())
                    .filter(u -> !u.getId().equals(idActual))
                    .ifPresent(
                            u -> {
                                throw new RegraNegocioException("Ja existe uma conta com o email " + email + ".");
                            });
        }
        if (codigo != null && !codigo.isBlank()) {
            utilizadores
                    .findByCodigoIgnoreCase(codigo.trim())
                    .filter(u -> !u.getId().equals(idActual))
                    .ifPresent(
                            u -> {
                                throw new RegraNegocioException("Ja existe uma conta com o codigo " + codigo + ".");
                            });
        }
    }

    private void validarDadosDoPerfil(
            PerfilUtilizador tipo, String curso, mz.co.isctem.tcc.dominio.enums.AreaCurso area, String departamento) {
        if (tipo == PerfilUtilizador.ESTUDANTE) {
            if (curso == null || curso.isBlank()) {
                throw new RegraNegocioException("O curso e obrigatorio para o perfil ESTUDANTE.");
            }
            if (area == null) {
                throw new RegraNegocioException(
                        "A area do curso e obrigatoria para o perfil ESTUDANTE (determina a existencia da Fase 5).");
            }
        }
        if (tipo == PerfilUtilizador.DOCENTE && (departamento == null || departamento.isBlank())) {
            throw new RegraNegocioException("O departamento e obrigatorio para o perfil DOCENTE.");
        }
    }

    private long contarAdministradoresActivos() {
        return utilizadores.findByTipo(PerfilUtilizador.ADMIN).stream().filter(Utilizador::activo).count();
    }

    private UtilizadorResposta enriquecer(Utilizador utilizador) {
        Estudante estudante =
                utilizador.getTipo() == PerfilUtilizador.ESTUDANTE
                        ? estudantes.findByUtilizadorId(utilizador.getId()).orElse(null)
                        : null;
        Docente docente =
                utilizador.getTipo() == PerfilUtilizador.DOCENTE
                        ? docentes.findByUtilizadorId(utilizador.getId()).orElse(null)
                        : null;
        return mapper.paraResposta(utilizador, estudante, docente);
    }

    private Utilizador carregar(Long id) {
        return utilizadores
                .findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Utilizador", id));
    }

    private Utilizador actor() {
        return utilizadores.findById(contexto.idUtilizadorActual()).orElse(null);
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
