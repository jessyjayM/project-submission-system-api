package mz.co.isctem.tcc.notificacao;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.comum.PaginaResposta;
import mz.co.isctem.tcc.dominio.enums.TipoNotificacao;
import mz.co.isctem.tcc.dominio.modelo.Notificacao;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.NotificacaoRepositorio;
import mz.co.isctem.tcc.notificacao.dto.NotificacaoResposta;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF09: notificacoes automaticas.
 *
 * <p>A notificacao in-app e sempre gravada; o email institucional e um canal complementar,
 * assincrono e opcional.
 */
@Service
@RequiredArgsConstructor
public class ServicoNotificacao {

    private final NotificacaoRepositorio repositorio;
    private final NotificacaoMapper mapper;
    private final EnviadorEmail enviadorEmail;

    @Transactional
    public void criar(
            Utilizador destinatario,
            TipoNotificacao tipo,
            String titulo,
            String conteudo,
            String ligacao) {
        if (destinatario == null) {
            return;
        }
        repositorio.save(
                Notificacao.builder()
                        .destinatario(destinatario)
                        .tipo(tipo)
                        .titulo(titulo)
                        .conteudo(conteudo)
                        .ligacao(ligacao)
                        .build());
        enviadorEmail.enviar(destinatario, titulo, conteudo);
    }

    @Transactional
    public void criarParaTodos(
            Collection<Utilizador> destinatarios,
            TipoNotificacao tipo,
            String titulo,
            String conteudo,
            String ligacao) {
        destinatarios.forEach(destinatario -> criar(destinatario, tipo, titulo, conteudo, ligacao));
    }

    @Transactional(readOnly = true)
    public PaginaResposta<NotificacaoResposta> listar(Long destinatarioId, Pageable pagina) {
        return PaginaResposta.de(
                repositorio.findByDestinatarioIdOrderByCriadaEmDesc(destinatarioId, pagina),
                mapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long destinatarioId) {
        return repositorio.countByDestinatarioIdAndLidaFalse(destinatarioId);
    }

    @Transactional
    public void marcarComoLida(Long notificacaoId, Long destinatarioId) {
        repositorio
                .findById(notificacaoId)
                .filter(n -> n.getDestinatario().getId().equals(destinatarioId))
                .ifPresent(n -> n.setLida(true));
    }

    @Transactional
    public int marcarTodasComoLidas(Long destinatarioId) {
        return repositorio.marcarTodasComoLidas(destinatarioId);
    }
}
