package mz.co.isctem.tcc.chat;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.chat.dto.MensagemResposta;
import mz.co.isctem.tcc.comum.PaginaResposta;
import mz.co.isctem.tcc.comum.excepcao.AcessoNegadoException;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.comum.excepcao.RegraNegocioException;
import mz.co.isctem.tcc.documento.ServicoArmazenamento;
import mz.co.isctem.tcc.dominio.enums.TipoNotificacao;
import mz.co.isctem.tcc.dominio.modelo.Mensagem;
import mz.co.isctem.tcc.dominio.modelo.Tcc;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.MensagemRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.TccRepositorio;
import mz.co.isctem.tcc.dominio.repositorio.UtilizadorRepositorio;
import mz.co.isctem.tcc.notificacao.ServicoNotificacao;
import mz.co.isctem.tcc.seguranca.UtilizadorAutenticado;
import mz.co.isctem.tcc.workflow.PoliticaAcessoTcc;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * RF07: canal privado de comunicacao entre cada estudante e o respectivo supervisor.
 *
 * <p>O Coordenador pode consultar as conversas para efeitos de acompanhamento institucional, mas
 * nao pode escrever, editar ou eliminar mensagens — nao existe qualquer operacao de escrita
 * acessivel a esse perfil nesta classe.
 */
@Service
@RequiredArgsConstructor
public class ServicoChat {

    private final MensagemRepositorio mensagens;
    private final TccRepositorio tccs;
    private final UtilizadorRepositorio utilizadores;
    private final ServicoArmazenamento armazenamento;
    private final ServicoNotificacao notificacoes;
    private final PoliticaAcessoTcc politica;

    @Transactional(readOnly = true)
    public PaginaResposta<MensagemResposta> listar(Long tccId, Pageable pagina) {
        politica.exigirLeitura(carregar(tccId));
        return PaginaResposta.de(mensagens.porTcc(tccId, pagina), ServicoChat::paraResposta);
    }

    /** Mensagens posteriores a {@code desdeId}, usadas pelo polling do frontend. */
    @Transactional(readOnly = true)
    public List<MensagemResposta> novasDesde(Long tccId, Long desdeId) {
        politica.exigirLeitura(carregar(tccId));
        return mensagens.novasDesde(tccId, desdeId == null ? 0L : desdeId).stream()
                .map(ServicoChat::paraResposta)
                .toList();
    }

    /**
     * Envia uma mensagem, com anexo opcional.
     *
     * <p>Apenas o estudante titular e o seu supervisor podem escrever no canal.
     */
    @Transactional
    public MensagemResposta enviar(Long tccId, String conteudo, MultipartFile anexo) {
        Tcc tcc = carregar(tccId);
        UtilizadorAutenticado actual = exigirParticipante(tcc);

        boolean temTexto = conteudo != null && !conteudo.isBlank();
        boolean temAnexo = anexo != null && !anexo.isEmpty();
        if (!temTexto && !temAnexo) {
            throw new RegraNegocioException("A mensagem tem de ter texto ou um anexo.");
        }

        Utilizador remetente =
                utilizadores
                        .findById(actual.id())
                        .orElseThrow(() -> RecursoNaoEncontradoException.de("Utilizador", actual.id()));

        Mensagem.MensagemBuilder construtor =
                Mensagem.builder()
                        .tcc(tcc)
                        .remetente(remetente)
                        .conteudo(temTexto ? conteudo.trim() : "(anexo)");

        if (temAnexo) {
            ServicoArmazenamento.FicheiroGuardado guardado =
                    armazenamento.guardar(anexo, "tcc-%d/chat".formatted(tccId));
            construtor
                    .anexoNome(guardado.nomeOriginal())
                    .anexoCaminho(guardado.caminhoRelativo())
                    .anexoTamanho(guardado.tamanho());
        }

        Mensagem gravada = mensagens.save(construtor.build());
        notificarDestinatario(tcc, remetente);
        return paraResposta(gravada);
    }

    @Transactional
    public int marcarComoLidas(Long tccId, Long leitorId) {
        politica.exigirLeitura(carregar(tccId));
        return mensagens.marcarComoLidas(tccId, leitorId);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long tccId, Long leitorId) {
        politica.exigirLeitura(carregar(tccId));
        return mensagens.contarNaoLidas(tccId, leitorId);
    }

    /** Anexo de uma mensagem, com a mesma politica de acesso do canal. */
    @Transactional(readOnly = true)
    public Mensagem mensagemComAnexo(Long mensagemId) {
        Mensagem mensagem =
                mensagens
                        .findById(mensagemId)
                        .orElseThrow(() -> RecursoNaoEncontradoException.de("Mensagem", mensagemId));
        if (!mensagem.temAnexo()) {
            throw new RecursoNaoEncontradoException("Esta mensagem nao tem anexo.");
        }
        politica.exigirLeitura(mensagem.getTcc());
        return mensagem;
    }

    /** O Coordenador tem acesso de leitura, mas nao participa na conversa. */
    private UtilizadorAutenticado exigirParticipante(Tcc tcc) {
        UtilizadorAutenticado actual = politica.exigirLeitura(tcc);
        if (actual.coordenador()) {
            throw new AcessoNegadoException(
                    "O Coordenador pode consultar as conversas, mas nao enviar mensagens.");
        }
        return actual;
    }

    private void notificarDestinatario(Tcc tcc, Utilizador remetente) {
        Utilizador estudante = tcc.getEstudante().getUtilizador();
        Utilizador destinatario =
                estudante.getId().equals(remetente.getId())
                        ? (tcc.getEstudante().temSupervisor()
                                ? tcc.getEstudante().getSupervisor().getUtilizador()
                                : null)
                        : estudante;
        notificacoes.criar(
                destinatario,
                TipoNotificacao.NOVA_MENSAGEM,
                "Nova mensagem de " + remetente.getNome(),
                "Tem uma nova mensagem no chat do TCC.",
                "/chat/" + tcc.getId());
    }

    private Tcc carregar(Long tccId) {
        return tccs.findById(tccId).orElseThrow(() -> RecursoNaoEncontradoException.de("TCC", tccId));
    }

    private static MensagemResposta paraResposta(Mensagem mensagem) {
        return new MensagemResposta(
                mensagem.getId(),
                mensagem.getRemetente().getId(),
                mensagem.getRemetente().getNome(),
                mensagem.getRemetente().getTipo(),
                mensagem.getConteudo(),
                mensagem.getAnexoNome(),
                mensagem.getAnexoTamanho(),
                mensagem.getEnviadaEm(),
                mensagem.getLidaEm());
    }
}
