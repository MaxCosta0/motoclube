package br.com.max.motoclube.shared.exception;

/** Violação de uma regra de negócio. Resulta em HTTP 422. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
