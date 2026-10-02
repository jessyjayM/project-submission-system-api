package mz.co.isctem.tcc.notificacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.config.PropriedadesIsctem;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Envio de email institucional (RF09), em bean separado para que o {@code @Async} passe pelo proxy
 * do Spring — uma chamada interna dentro do mesmo bean seria executada de forma sincrona.
 *
 * <p>Desligado por defeito ate haver credenciais SMTP do ISCTEM. Uma falha de envio e registada mas
 * nunca propaga: a notificacao in-app ja foi gravada e e o canal garantido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnviadorEmail {

    private final PropriedadesIsctem propriedades;
    private final JavaMailSender remetente;

    @Async
    public void enviar(Utilizador destinatario, String assunto, String corpo) {
        if (!propriedades.notificacoes().emailActivo()
                || destinatario.getEmail() == null
                || destinatario.getEmail().isBlank()) {
            return;
        }
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(propriedades.notificacoes().remetente());
            mensagem.setTo(destinatario.getEmail());
            mensagem.setSubject("[ISCTEM TCC] " + assunto);
            mensagem.setText(corpo);
            remetente.send(mensagem);
        } catch (Exception e) {
            log.warn(
                    "Falha ao enviar email de notificacao para {}: {}",
                    destinatario.getEmail(),
                    e.getMessage());
        }
    }
}
