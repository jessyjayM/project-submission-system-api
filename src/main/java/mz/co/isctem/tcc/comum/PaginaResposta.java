package mz.co.isctem.tcc.comum;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Envelope de paginacao devolvido pela API. */
public record PaginaResposta<T>(
        List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas, boolean ultima) {

    public static <E, T> PaginaResposta<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginaResposta<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isLast());
    }
}
