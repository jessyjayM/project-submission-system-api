package mz.co.isctem.tcc.comum;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.comum.excepcao.AcessoNegadoException;
import mz.co.isctem.tcc.comum.excepcao.RecursoNaoEncontradoException;
import mz.co.isctem.tcc.comum.excepcao.RegraNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Converte as excepcoes do dominio em respostas HTTP consistentes. */
@Slf4j
@RestControllerAdvice
public class ManipuladorGlobalExcepcoes {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<RespostaErro> naoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest pedido) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), pedido);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<RespostaErro> regraNegocio(RegraNegocioException ex, HttpServletRequest pedido) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), pedido);
    }

    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ResponseEntity<RespostaErro> acessoNegado(Exception ex, HttpServletRequest pedido) {
        return responder(
                HttpStatus.FORBIDDEN,
                ex.getMessage() == null ? "Sem permissao para esta operacao." : ex.getMessage(),
                pedido);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespostaErro> naoAutenticado(AuthenticationException ex, HttpServletRequest pedido) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), pedido);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespostaErro> pedidoInvalido(IllegalArgumentException ex, HttpServletRequest pedido) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), pedido);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RespostaErro> ficheiroDemasiadoGrande(
            MaxUploadSizeExceededException ex, HttpServletRequest pedido) {
        return responder(
                HttpStatus.PAYLOAD_TOO_LARGE, "O ficheiro excede o tamanho maximo permitido.", pedido);
    }

    /**
     * Restricoes da base de dados (unicidade, chaves estrangeiras) sao regras de negocio, nao falhas
     * do servidor: devolver 500 esconderia do utilizador o que realmente aconteceu.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespostaErro> integridade(
            DataIntegrityViolationException ex, HttpServletRequest pedido) {
        log.warn("Violacao de integridade em {} {}", pedido.getMethod(), pedido.getRequestURI(), ex);
        return responder(
                HttpStatus.CONFLICT,
                "A operacao viola uma restricao de integridade dos dados.",
                pedido);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> validacao(
            MethodArgumentNotValidException ex, HttpServletRequest pedido) {
        Map<String, List<String>> campos = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(
                        erro ->
                                campos.computeIfAbsent(erro.getField(), k -> new ArrayList<>())
                                        .add(erro.getDefaultMessage()));
        RespostaErro corpo =
                new RespostaErro(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Existem campos invalidos no pedido.",
                        pedido.getRequestURI(),
                        campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> erroInesperado(Exception ex, HttpServletRequest pedido) {
        log.error("Erro inesperado em {} {}", pedido.getMethod(), pedido.getRequestURI(), ex);
        return responder(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado no servidor.", pedido);
    }

    private ResponseEntity<RespostaErro> responder(
            HttpStatus estado, String mensagem, HttpServletRequest pedido) {
        return ResponseEntity.status(estado)
                .body(
                        RespostaErro.de(
                                estado.value(), estado.getReasonPhrase(), mensagem, pedido.getRequestURI()));
    }
}
