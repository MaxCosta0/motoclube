package br.com.max.motoclube.shared.exception;

/** Recurso inexistente. Resulta em HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException("%s %s não encontrado(a).".formatted(recurso, id));
    }
}
