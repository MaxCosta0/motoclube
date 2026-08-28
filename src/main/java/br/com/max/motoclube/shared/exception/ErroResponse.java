package br.com.max.motoclube.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        OffsetDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        List<CampoInvalido> campos) {

    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(OffsetDateTime.now(), status, erro, mensagem, null);
    }

    public static ErroResponse deValidacao(int status, String mensagem, Map<String, String> erros) {
        List<CampoInvalido> campos = erros.entrySet().stream()
                .map(e -> new CampoInvalido(e.getKey(), e.getValue()))
                .toList();
        return new ErroResponse(OffsetDateTime.now(), status, "Requisição inválida", mensagem, campos);
    }

    public record CampoInvalido(String campo, String mensagem) {}
}
